package br.gov.quixada.sports.security.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Login rate limit settings ({@code POST /v1/auth/login}).
 * <p>
 * {@code maxPerIp} protects against spraying and CPU DoS (each attempt costs a BCrypt);
 * {@code maxPerIpEmail} protects a specific account against brute force. An isolated
 * per-email limit does not exist on purpose: it would allow locking a third party's account (DoS).
 */
@Validated
@ConfigurationProperties(prefix = "api.security.login-rate-limit")
public record LoginRateLimitProperties(
        @Min(1) long maxPerIp,
        @Min(1) long maxPerIpEmail,
        @NotNull @DurationMin(seconds = 1) Duration window,
        @Min(1) long maxKeys
) {
}
