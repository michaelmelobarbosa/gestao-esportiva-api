package br.gov.quixada.esporte.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import br.gov.quixada.esporte.security.auth.exception.TokenInvalidoException;
import br.gov.quixada.esporte.security.users.User;
import br.gov.quixada.esporte.security.users.UserRole;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET = "segredo-de-teste-com-tamanho-suficiente-1234567890";
    private static final String ISSUER = "gestao-esportiva-api";

    private JwtService service;

    @BeforeEach
    void setUp() {
        var properties = new TokenProperties(SECRET, ISSUER, Duration.ofHours(2));
        var clock = Clock.fixed(Instant.now(), ZoneId.of("America/Fortaleza"));
        service = new JwtService(properties, clock);
    }

    @Test
    @DisplayName("deve gerar token e extrair o email")
    void deveGerarEExtrairEmail() {
        var user = new User("fulano", "fulano@example.com", "hash", UserRole.USER);

        var token = service.generateToken(user);

        assertThat(service.extractEmail(token)).isEqualTo("fulano@example.com");
    }

    @Test
    @DisplayName("deve lançar TokenInvalidoException quando a assinatura não confere")
    void deveLancarQuandoAssinaturaNaoConfere() {
        var token = JWT.create()
                .withIssuer(ISSUER)
                .withSubject("fulano@example.com")
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.HMAC256("outro-segredo"));

        assertThatThrownBy(() -> service.extractEmail(token))
                .isInstanceOf(TokenInvalidoException.class)
                .hasMessage("Token inválido ou expirado");
    }

    @Test
    @DisplayName("deve lançar TokenInvalidoException quando o token estiver expirado")
    void deveLancarQuandoTokenExpirado() {
        var token = JWT.create()
                .withIssuer(ISSUER)
                .withSubject("fulano@example.com")
                .withExpiresAt(Instant.now().minusSeconds(60))
                .sign(Algorithm.HMAC256(SECRET));

        assertThatThrownBy(() -> service.extractEmail(token))
                .isInstanceOf(TokenInvalidoException.class)
                .hasMessage("Token inválido ou expirado");
    }

    @Test
    @DisplayName("deve lançar TokenInvalidoException quando o token for malformado")
    void deveLancarQuandoTokenMalformado() {
        assertThatThrownBy(() -> service.extractEmail("lixo"))
                .isInstanceOf(TokenInvalidoException.class)
                .hasMessage("Token inválido ou expirado");
    }
}
