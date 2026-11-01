package br.gov.quixada.esporte.security.config;

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

import br.gov.quixada.esporte.security.auth.UserRepository;
import br.gov.quixada.esporte.security.auth.exception.TokenInvalidoException;
import br.gov.quixada.esporte.security.users.User;
import br.gov.quixada.esporte.security.users.UserRole;

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
    @DisplayName("deve autenticar quando o token for válido")
    void deveAutenticarComTokenValido() throws Exception {
        var user = new User("fulano", "fulano@example.com", "hash", UserRole.USER);
        when(jwtService.extractEmail("token")).thenReturn("fulano@example.com");
        when(userRepository.findByEmail("fulano@example.com")).thenReturn(user);

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        var chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isSameAs(user);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("deve seguir sem autenticação quando o token for inválido")
    void deveSeguirSemAutenticacaoComTokenInvalido() throws Exception {
        when(jwtService.extractEmail("token")).thenThrow(new TokenInvalidoException("Token inválido ou expirado"));

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        var chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("deve ignorar header sem o esquema Bearer")
    void deveIgnorarHeaderSemBearer() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Token abc");
        var chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(jwtService, never()).extractEmail(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("deve seguir sem autenticação quando o usuário do token não existir")
    void deveSeguirSemAutenticacaoQuandoUsuarioNaoExistir() throws Exception {
        when(jwtService.extractEmail("token")).thenReturn("sumiu@example.com");
        when(userRepository.findByEmail("sumiu@example.com")).thenReturn(null);

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        var chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }
}
