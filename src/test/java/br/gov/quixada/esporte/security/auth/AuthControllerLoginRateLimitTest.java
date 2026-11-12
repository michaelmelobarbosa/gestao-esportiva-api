package br.gov.quixada.esporte.security.auth;

import br.gov.quixada.esporte.security.config.LoginRateLimitProperties;
import br.gov.quixada.esporte.security.jwt.JwtService;
import br.gov.quixada.esporte.security.users.User;
import br.gov.quixada.esporte.security.users.UserDetailsAdapter;
import br.gov.quixada.esporte.security.users.UserRole;
import br.gov.quixada.esporte.support.ClockTimeMeter;
import br.gov.quixada.esporte.support.ControllerTestSupport;
import br.gov.quixada.esporte.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController - rate limit de login")
class AuthControllerLoginRateLimitTest {

    private static final String IP = "10.0.0.1";
    private static final String OUTRO_IP = "10.0.0.2";
    private static final Duration JANELA = Duration.ofMinutes(15);

    @Mock
    private AuthService authService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private MutableClock clock;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(ControllerTestSupport.FIXED_INSTANT, ControllerTestSupport.FIXED_ZONE);
        configurar(100, 2);
    }

    @Test
    @DisplayName("deve retornar 429 com Retry-After após exceder as tentativas")
    void deveRetornar429AposExcederTentativas() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        mockMvc.perform(loginDe(IP)).andExpect(status().isUnauthorized());
        mockMvc.perform(loginDe(IP)).andExpect(status().isUnauthorized());

        var resultado = mockMvc.perform(loginDe(IP))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Muitas tentativas de login. Tente novamente em instantes."))
                .andExpect(jsonPath("$.path").value("/v1/auth/login"))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andReturn();

        var retryAfter = resultado.getResponse().getHeader(HttpHeaders.RETRY_AFTER);
        assertThat(retryAfter).isNotNull();
        assertThat(Long.parseLong(retryAfter)).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("não deve bloquear outro IP")
    void naoDeveBloquearOutroIp() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        mockMvc.perform(loginDe(IP)).andExpect(status().isUnauthorized());
        mockMvc.perform(loginDe(IP)).andExpect(status().isUnauthorized());
        mockMvc.perform(loginDe(IP)).andExpect(status().isTooManyRequests());

        mockMvc.perform(loginDe(OUTRO_IP)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("deve devolver a tentativa quando o login tem sucesso")
    void deveDevolverTentativaNoSucesso() throws Exception {
        var user = new User("fulano", "fulano@example.com", "hash", UserRole.USER);
        var userDetails = new UserDetailsAdapter(user);
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"))
                .thenReturn(authentication)
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));
        when(jwtService.generateToken(user)).thenReturn("token");

        mockMvc.perform(loginDe(IP)).andExpect(status().isUnauthorized());
        mockMvc.perform(loginDe(IP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"));
        mockMvc.perform(loginDe(IP)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("deve liberar novamente quando a janela expira")
    void deveLiberarQuandoJanelaExpira() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        mockMvc.perform(loginDe(IP)).andExpect(status().isUnauthorized());
        mockMvc.perform(loginDe(IP)).andExpect(status().isUnauthorized());

        clock.advance(JANELA);

        mockMvc.perform(loginDe(IP)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("deve limitar pelo bucket de IP com e-mails distintos")
    void deveLimitarPeloBucketDeIp() throws Exception {
        configurar(2, 100);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        mockMvc.perform(loginDe(IP, "a@example.com")).andExpect(status().isUnauthorized());
        mockMvc.perform(loginDe(IP, "b@example.com")).andExpect(status().isUnauthorized());
        mockMvc.perform(loginDe(IP, "c@example.com")).andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("não deve consumir tentativa quando o corpo é inválido")
    void naoDeveConsumirTentativaComCorpoInvalido() throws Exception {
        var corpoInvalido = """
                {"email":"nao-email","password":"senha123"}
                """;

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoInvalido))
                    .andExpect(status().isBadRequest());
        }

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        mockMvc.perform(loginDe(IP)).andExpect(status().isUnauthorized());
        mockMvc.perform(loginDe(IP)).andExpect(status().isUnauthorized());
    }

    private void configurar(long maxPorIp, long maxPorIpEmail) {
        var propriedades = new LoginRateLimitProperties(maxPorIp, maxPorIpEmail, JANELA, 1000);
        var limiter = new LoginRateLimiter(propriedades, new ClockTimeMeter(clock));
        var controller = new AuthController(authService, authenticationManager, jwtService, limiter);
        mockMvc = ControllerTestSupport.mockMvc(controller);
    }

    private MockHttpServletRequestBuilder loginDe(String ip) {
        return loginDe(ip, "fulano@example.com");
    }

    private MockHttpServletRequestBuilder loginDe(String ip, String email) {
        var corpo = """
                {"email":"%s","password":"senha123"}
                """.formatted(email);

        return post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo)
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                });
    }
}
