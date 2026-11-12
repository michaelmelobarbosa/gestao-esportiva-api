package br.gov.quixada.sports.security.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Origins allowed to call the API from a browser. Configurable per profile:
 * {@code api.cors.allowed-origins} (comma-separated list, via
 * {@code API_CORS_ALLOWED_ORIGINS} in production).
 */
@Validated
@ConfigurationProperties(prefix = "api.cors")
public record CorsProperties(
        @NotNull List<@NotBlank String> allowedOrigins
) {
}
