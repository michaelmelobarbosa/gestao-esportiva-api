package br.gov.quixada.esporte.security.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.gov.quixada.esporte.common.error.GlobalExceptionHandler;
import br.gov.quixada.esporte.security.auth.exception.EmailJaCadastradoException;
import br.gov.quixada.esporte.security.config.JwtService;
import br.gov.quixada.esporte.security.users.User;
import br.gov.quixada.esporte.security.users.UserRole;
import br.gov.quixada.esporte.security.users.dto.RegisterRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    private static final String LOGIN_JSON = """
            {"email":"fulano@example.com","password":"senha123"}
            """;

    private static final String REGISTER_JSON = """
            {"username":"fulano","email":"fulano@example.com","password":"senha123"}
            """;

    @Mock
    private AuthService authService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new AuthController(authService, authenticationManager, jwtService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("deve autenticar usando o email e retornar o token")
        void deveAutenticarComEmail() throws Exception {
            var user = new User("fulano", "fulano@example.com", "hash", UserRole.USER);
            var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(jwtService.generateToken(user)).thenReturn("token");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("token"));

            var captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("fulano@example.com");
        }

        @Test
        @DisplayName("deve retornar 401 quando as credenciais forem inválidas")
        void deveRetornar401QuandoCredenciaisInvalidas() throws Exception {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Credenciais inválidas"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
        }

        @Test
        @DisplayName("deve retornar 400 quando o email for inválido")
        void deveRetornar400QuandoEmailInvalido() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"nao-email","password":"senha123"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("email"));
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("deve delegar o cadastro para o AuthService")
        void deveDelegarCadastro() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REGISTER_JSON))
                    .andExpect(status().isOk());

            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("deve retornar 409 quando o email já estiver cadastrado")
        void deveRetornar409QuandoEmailJaCadastrado() throws Exception {
            doThrow(new EmailJaCadastradoException("Email já cadastrado"))
                    .when(authService).register(any(RegisterRequest.class));

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REGISTER_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Email já cadastrado"));
        }
    }
}
