package br.gov.quixada.sports.security.jwt;

import br.gov.quixada.sports.security.auth.UserRepository;
import br.gov.quixada.sports.security.auth.exception.InvalidRefreshTokenException;
import br.gov.quixada.sports.security.users.User;
import br.gov.quixada.sports.security.users.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;
    private final TokenProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public record RotatedTokens(User user, String refreshToken) {
    }

    @Transactional
    public String issue(User user) {
        String rawToken = generateRawToken();
        LocalDateTime now = LocalDateTime.now(clock);
        var refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256Hex(rawToken))
                .createdAt(now)
                .expiresAt(now.plus(properties.refreshExpiration()))
                .build();
        repository.save(refreshToken);
        return rawToken;
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotatedTokens rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }

        var token = repository.findByTokenHash(sha256Hex(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid or expired refresh token"));

        LocalDateTime now = LocalDateTime.now(clock);

        if (token.isRevoked()) {
            var user = token.getUser();
            user.incrementTokenVersion();
            userRepository.saveAndFlush(user);
            repository.revokeAllActiveByUserId(user.getId(), now);
            throw new InvalidRefreshTokenException("Refresh token has been revoked");
        }

        if (token.isExpired(now)) {
            token.revoke(now);
            repository.save(token);
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        if (token.getUser().getStatus() != UserStatus.ACTIVE) {
            token.revoke(now);
            repository.save(token);
            throw new InvalidRefreshTokenException("User account is inactive");
        }

        token.revoke(now);
        repository.save(token);
        String newRaw = issue(token.getUser());
        return new RotatedTokens(token.getUser(), newRaw);
    }

    @Transactional
    public void revokeAllForUser(User user) {
        repository.revokeAllActiveByUserId(user.getId(), LocalDateTime.now(clock));
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
