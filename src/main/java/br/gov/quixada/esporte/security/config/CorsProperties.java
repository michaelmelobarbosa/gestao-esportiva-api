package br.gov.quixada.esporte.security.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Origens autorizadas a chamar a API pelo navegador. Configurável por perfil:
 * {@code api.cors.allowed-origins} (lista separada por vírgula, via
 * {@code API_CORS_ALLOWED_ORIGINS} em produção).
 */
@Validated
@ConfigurationProperties(prefix = "api.cors")
public record CorsProperties(
        @NotNull List<@NotBlank String> allowedOrigins
) {
}
