package br.gov.quixada.sports.security.auth;

import br.gov.quixada.sports.security.config.LoginRateLimitProperties;
import br.gov.quixada.sports.security.jwt.JwtService;
import br.gov.quixada.sports.security.users.User;
import br.gov.quixada.sports.security.users.UserDetailsAdapter;
import br.gov.quixada.sports.security.users.UserRole;
import br.gov.quixada.sports.support.ClockTimeMeter;
import br.gov.quixada.sports.support.ControllerTestSupport;
import br.gov.quixada.sports.support.MutableClock;
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
@DisplayName("AuthController - login rate limit")
class AuthControllerLoginRateLimitTest {

    private static final String IP = "10.0.0.1";
    private static final String OTHER_IP = "10.0.0.2";
    private static final Duration WINDOW = Duration.ofMinutes(15);

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
        configure(100, 2);
    }

    @Test
    @DisplayName("should return 429 with Retry-After after exceeding the attempts")
    void shouldReturn429AfterExceedingAttempts() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(loginFrom(IP)).andExpect(status().isUnauthorized());
        mockMvc.perform(loginFrom(IP)).andExpect(status().isUnauthorized());

        var result = mockMvc.perform(loginFrom(IP))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Too many login attempts. Try again shortly."))
                .andExpect(jsonPath("$.path").value("/v1/auth/login"))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andReturn();

        var retryAfter = result.getResponse().getHeader(HttpHeaders.RETRY_AFTER);
        assertThat(retryAfter).isNotNull();
        assertThat(Long.parseLong(retryAfter)).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("should not block another IP")
    void shouldNotBlockOtherIp() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(loginFrom(IP)).andExpect(status().isUnauthorized());
        mockMvc.perform(loginFrom(IP)).andExpect(status().isUnauthorized());
        mockMvc.perform(loginFrom(IP)).andExpect(status().isTooManyRequests());

        mockMvc.perform(loginFrom(OTHER_IP)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return the attempt when the login succeeds")
    void shouldReturnAttemptOnSuccess() throws Exception {
        var user = new User("john", "john@example.com", "hash", UserRole.USER);
        var userDetails = new UserDetailsAdapter(user);
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"))
                .thenReturn(authentication)
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        when(jwtService.generateToken(user)).thenReturn("token");

        mockMvc.perform(loginFrom(IP)).andExpect(status().isUnauthorized());
        mockMvc.perform(loginFrom(IP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"));
        mockMvc.perform(loginFrom(IP)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should free again when the window expires")
    void shouldFreeAgainWhenWindowExpires() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(loginFrom(IP)).andExpect(status().isUnauthorized());
        mockMvc.perform(loginFrom(IP)).andExpect(status().isUnauthorized());

        clock.advance(WINDOW);

        mockMvc.perform(loginFrom(IP)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should limit by the IP bucket across distinct emails")
    void shouldLimitByIpBucket() throws Exception {
        configure(2, 100);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(loginFrom(IP, "a@example.com")).andExpect(status().isUnauthorized());
        mockMvc.perform(loginFrom(IP, "b@example.com")).andExpect(status().isUnauthorized());
        mockMvc.perform(loginFrom(IP, "c@example.com")).andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("should not consume an attempt when the body is invalid")
    void shouldNotConsumeAttemptWithInvalidBody() throws Exception {
        var invalidBody = """
                {"email":"not-an-email","password":"password123"}
                """;

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(loginFrom(IP)).andExpect(status().isUnauthorized());
        mockMvc.perform(loginFrom(IP)).andExpect(status().isUnauthorized());
    }

    private void configure(long maxPerIp, long maxPerIpEmail) {
        var properties = new LoginRateLimitProperties(maxPerIp, maxPerIpEmail, WINDOW, 1000);
        var limiter = new LoginRateLimiter(properties, new ClockTimeMeter(clock));
        var controller = new AuthController(authService, authenticationManager, jwtService, limiter);
        mockMvc = ControllerTestSupport.mockMvc(controller);
    }

    private MockHttpServletRequestBuilder loginFrom(String ip) {
        return loginFrom(ip, "john@example.com");
    }

    private MockHttpServletRequestBuilder loginFrom(String ip, String email) {
        var body = """
                {"email":"%s","password":"password123"}
                """.formatted(email);

        return post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                });
    }
}
