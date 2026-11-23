package br.gov.quixada.sports.security.jwt;

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

import br.gov.quixada.sports.security.auth.exception.InvalidTokenException;
import br.gov.quixada.sports.security.users.User;
import br.gov.quixada.sports.security.users.UserRole;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET = "segredo-de-teste-com-tamanho-suficiente-1234567890";
    private static final String ISSUER = "gestao-esportiva-api";

    private JwtService service;

    @BeforeEach
    void setUp() {
        var properties = new TokenProperties(SECRET, ISSUER, Duration.ofHours(2), Duration.ofDays(7));
        var clock = Clock.fixed(Instant.now(), ZoneId.of("America/Fortaleza"));
        service = new JwtService(properties, clock);
    }

    @Test
    @DisplayName("should generate token with ver and role claims and extract email and claims")
    void shouldGenerateAndExtractEmail() {
        var user = new User("john", "john@example.com", "hash", UserRole.ADMIN);
        user.incrementTokenVersion();
        user.incrementTokenVersion();

        var token = service.generateToken(user);

        assertThat(service.extractEmail(token)).isEqualTo("john@example.com");
        var claims = service.extractClaims(token);
        assertThat(claims.email()).isEqualTo("john@example.com");
        assertThat(claims.tokenVersion()).isEqualTo(2);

        var decoded = JWT.decode(token);
        assertThat(decoded.getClaim("ver").asInt()).isEqualTo(2);
        assertThat(decoded.getClaim("role").asString()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("should throw InvalidTokenException when ver claim is missing")
    void shouldThrowWhenVerClaimIsMissing() {
        var token = JWT.create()
                .withIssuer(ISSUER)
                .withSubject("john@example.com")
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.HMAC256(SECRET));

        assertThatThrownBy(() -> service.extractClaims(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid or expired token");
    }

    @Test
    @DisplayName("should throw InvalidTokenException when the signature does not match")
    void shouldThrowWhenSignatureDoesNotMatch() {
        var token = JWT.create()
                .withIssuer(ISSUER)
                .withSubject("john@example.com")
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.HMAC256("outro-segredo"));

        assertThatThrownBy(() -> service.extractEmail(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid or expired token");
    }

    @Test
    @DisplayName("should throw InvalidTokenException when the token is expired")
    void shouldThrowWhenTokenExpired() {
        var token = JWT.create()
                .withIssuer(ISSUER)
                .withSubject("john@example.com")
                .withExpiresAt(Instant.now().minusSeconds(60))
                .sign(Algorithm.HMAC256(SECRET));

        assertThatThrownBy(() -> service.extractEmail(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid or expired token");
    }

    @Test
    @DisplayName("should throw InvalidTokenException when the token is malformed")
    void shouldThrowWhenTokenMalformed() {
        assertThatThrownBy(() -> service.extractEmail("garbage"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid or expired token");
    }
}
