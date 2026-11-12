package br.gov.quixada.sports.security.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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

import java.util.Optional;

import br.gov.quixada.sports.security.auth.exception.EmailAlreadyRegisteredException;
import br.gov.quixada.sports.security.jwt.JwtService;
import br.gov.quixada.sports.security.users.User;
import br.gov.quixada.sports.security.users.UserDetailsAdapter;
import br.gov.quixada.sports.security.users.UserRole;
import br.gov.quixada.sports.security.users.dto.RegisterRequest;
import br.gov.quixada.sports.support.ControllerTestSupport;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    private static final String LOGIN_JSON = """
            {"email":"john@example.com","password":"password123"}
            """;

    private static final String REGISTER_JSON = """
            {"username":"john","email":"john@example.com","password":"password123"}
            """;

    @Mock
    private AuthService authService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new AuthController(authService, authenticationManager, jwtService, loginRateLimiter);
        mockMvc = ControllerTestSupport.mockMvc(controller);
        lenient().when(loginRateLimiter.attempt(any(), any())).thenReturn(Optional.empty());
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("should authenticate using the email and return the token")
        void shouldAuthenticateWithEmail() throws Exception {
            var user = new User("john", "john@example.com", "hash", UserRole.USER);
            var userDetails = new UserDetailsAdapter(user);
            var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(jwtService.generateToken(user)).thenReturn("token");

            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("token"));

            var captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should return 401 when credentials are invalid")
        void shouldReturn401WhenCredentialsInvalid() throws Exception {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid credentials"));
        }

        @Test
        @DisplayName("should return 400 when the email is invalid")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"not-an-email","password":"password123"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("email"));
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should delegate registration to the AuthService")
        void shouldDelegateRegistration() throws Exception {
            mockMvc.perform(post("/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REGISTER_JSON))
                    .andExpect(status().isOk());

            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("should return 409 when the email is already registered")
        void shouldReturn409WhenEmailAlreadyRegistered() throws Exception {
            doThrow(new EmailAlreadyRegisteredException("Email already registered"))
                    .when(authService).register(any(RegisterRequest.class));

            mockMvc.perform(post("/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REGISTER_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Email already registered"));
        }
    }

    @Nested
    @DisplayName("versioning")
    class Versioning {

        @Test
        @DisplayName("should reject unsupported version with 400")
        void shouldRejectUnsupportedVersion() throws Exception {
            mockMvc.perform(post("/v9/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_JSON))
                    .andExpect(status().isBadRequest());
        }
    }
}
