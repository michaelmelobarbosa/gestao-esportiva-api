package br.gov.quixada.sports.security.jwt;

import br.gov.quixada.sports.security.auth.UserRepository;
import br.gov.quixada.sports.security.auth.exception.InvalidRefreshTokenException;
import br.gov.quixada.sports.security.users.User;
import br.gov.quixada.sports.security.users.UserRole;
import br.gov.quixada.sports.security.users.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    private static final String SECRET = "segredo-de-teste-com-tamanho-suficiente-1234567890";
    private static final String ISSUER = "sports-management-api";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-15T12:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private UserRepository userRepository;

    private Clock clock;
    private LocalDateTime now;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_INSTANT, ZONE);
        now = LocalDateTime.now(clock);
        var properties = new TokenProperties(SECRET, ISSUER, Duration.ofHours(2), Duration.ofDays(7));
        service = new RefreshTokenService(repository, userRepository, properties, clock);
    }

    private User activeUser() {
        return new User(1L, "john", "john@example.com", "hash", UserRole.USER, UserStatus.ACTIVE);
    }

    private static String sha256Hex(String rawToken) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("should issue raw token and persist only its SHA-256 hex digest")
    void shouldIssueAndPersistSha256HashOnly() {
        var user = activeUser();

        String rawToken = service.issue(user);

        assertThat(rawToken).isNotBlank();
        var captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getTokenHash())
                .hasSize(64)
                .isNotEqualTo(rawToken)
                .isEqualTo(sha256Hex(rawToken));
        assertThat(saved.getCreatedAt()).isEqualTo(now);
        assertThat(saved.getExpiresAt()).isEqualTo(now.plusDays(7));
        assertThat(saved.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("should rotate a valid refresh token, revoking the old one and issuing a new one")
    void shouldRotateValidRefreshToken() {
        var user = activeUser();
        String oldRaw = "old-raw-refresh-token";
        var existing = RefreshToken.builder()
                .id(10L)
                .user(user)
                .tokenHash(sha256Hex(oldRaw))
                .createdAt(now.minusHours(1))
                .expiresAt(now.plusDays(6))
                .build();
        when(repository.findByTokenHash(sha256Hex(oldRaw))).thenReturn(Optional.of(existing));

        var rotated = service.rotate(oldRaw);

        assertThat(rotated.user()).isSameAs(user);
        assertThat(rotated.refreshToken()).isNotBlank().isNotEqualTo(oldRaw);
        assertThat(existing.isRevoked()).isTrue();
        assertThat(existing.getRevokedAt()).isEqualTo(now);

        var captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0)).isSameAs(existing);
        assertThat(captor.getAllValues().get(1).getTokenHash()).isEqualTo(sha256Hex(rotated.refreshToken()));
    }

    @Test
    @DisplayName("should detect reuse of a revoked token, revoke all active tokens, increment tokenVersion, and throw")
    void shouldDetectReuseAndRevokeFamily() {
        var user = activeUser();
        String reusedRaw = "reused-raw-token";
        var revokedToken = RefreshToken.builder()
                .id(10L)
                .user(user)
                .tokenHash(sha256Hex(reusedRaw))
                .createdAt(now.minusHours(2))
                .expiresAt(now.plusDays(5))
                .revokedAt(now.minusMinutes(30))
                .build();
        when(repository.findByTokenHash(sha256Hex(reusedRaw))).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> service.rotate(reusedRaw))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token has been revoked");

        assertThat(user.getTokenVersion()).isEqualTo(1);
        verify(userRepository).saveAndFlush(user);
        verify(repository).revokeAllActiveByUserId(user.getId(), now);
        verify(repository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("should reject an expired refresh token and mark it revoked")
    void shouldRejectExpiredToken() {
        var user = activeUser();
        String expiredRaw = "expired-raw-token";
        var expiredToken = RefreshToken.builder()
                .id(11L)
                .user(user)
                .tokenHash(sha256Hex(expiredRaw))
                .createdAt(now.minusDays(8))
                .expiresAt(now)
                .build();
        when(repository.findByTokenHash(sha256Hex(expiredRaw))).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> service.rotate(expiredRaw))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token has expired");

        assertThat(expiredToken.isRevoked()).isTrue();
        assertThat(expiredToken.getRevokedAt()).isEqualTo(now);
        verify(repository).save(expiredToken);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("should reject refresh token when user account is inactive and mark token revoked")
    void shouldRejectInactiveUser() {
        var user = new User(1L, "john", "john@example.com", "hash", UserRole.USER, UserStatus.INACTIVE);
        String raw = "inactive-user-token";
        var token = RefreshToken.builder()
                .id(12L)
                .user(user)
                .tokenHash(sha256Hex(raw))
                .createdAt(now.minusHours(1))
                .expiresAt(now.plusDays(6))
                .build();
        when(repository.findByTokenHash(sha256Hex(raw))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.rotate(raw))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("User account is inactive");

        assertThat(token.isRevoked()).isTrue();
        verify(repository).save(token);
    }

    @Test
    @DisplayName("should reject blank or unknown refresh tokens")
    void shouldRejectBlankOrUnknownTokens() {
        assertThatThrownBy(() -> service.rotate("   "))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token");

        when(repository.findByTokenHash(sha256Hex("unknown"))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.rotate("unknown"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    @DisplayName("should revoke all active refresh tokens for a user")
    void shouldRevokeAllForUser() {
        var user = activeUser();

        service.revokeAllForUser(user);

        verify(repository).revokeAllActiveByUserId(user.getId(), now);
    }
}
