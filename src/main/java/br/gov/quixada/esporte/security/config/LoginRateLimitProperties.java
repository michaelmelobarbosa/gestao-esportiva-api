package br.gov.quixada.esporte.security.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Limites do rate limit de login ({@code POST /v1/auth/login}).
 * <p>
 * {@code maxPorIp} protege contra spraying e DoS de CPU (cada tentativa custa um BCrypt);
 * {@code maxPorIpEmail} protege uma conta específica contra força bruta. O limite por e-mail
 * isolado não existe de propósito: permitiria bloquear a conta de terceiros (DoS).
 */
@Validated
@ConfigurationProperties(prefix = "api.security.login-rate-limit")
public record LoginRateLimitProperties(
        @Min(1) long maxPorIp,
        @Min(1) long maxPorIpEmail,
        @NotNull @DurationMin(seconds = 1) Duration janela,
        @Min(1) long maximoChaves
) {
}
