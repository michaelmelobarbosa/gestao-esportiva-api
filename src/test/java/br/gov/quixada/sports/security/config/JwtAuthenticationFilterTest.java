package br.gov.quixada.sports.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import br.gov.quixada.sports.security.auth.UserRepository;
import br.gov.quixada.sports.security.auth.exception.InvalidTokenException;
import br.gov.quixada.sports.security.jwt.JwtService;
import br.gov.quixada.sports.security.users.User;
import br.gov.quixada.sports.security.users.UserDetailsAdapter;
import br.gov.quixada.sports.security.users.UserRole;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should authenticate when the token is valid")
    void shouldAuthenticateWithValidToken() throws Exception {
        var user = new User("john", "john@example.com", "hash", UserRole.USER);
        when(jwtService.extractEmail("token")).thenReturn("john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        var chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(UserDetailsAdapter.class);
        assertThat(((UserDetailsAdapter) authentication.getPrincipal()).user()).isSameAs(user);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("should continue without authentication when the token is invalid")
    void shouldContinueWithoutAuthenticationWithInvalidToken() throws Exception {
        when(jwtService.extractEmail("token")).thenThrow(new InvalidTokenException("Invalid or expired token"));

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        var chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("should ignore header without the Bearer scheme")
    void shouldIgnoreHeaderWithoutBearer() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Token abc");
        var chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(jwtService, never()).extractEmail(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("should continue without authentication when the token user does not exist")
    void shouldContinueWithoutAuthenticationWhenUserDoesNotExist() throws Exception {
        when(jwtService.extractEmail("token")).thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        var chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("should not authenticate an inactive user")
    void shouldNotAuthenticateInactiveUser() throws Exception {
        var user = new User("john", "john@example.com", "hash", UserRole.USER);
        user.setStatus(br.gov.quixada.sports.security.users.UserStatus.INACTIVE);
        when(jwtService.extractEmail("token")).thenReturn("john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        var chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
