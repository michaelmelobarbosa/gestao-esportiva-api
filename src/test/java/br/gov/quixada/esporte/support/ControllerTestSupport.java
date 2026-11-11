package br.gov.quixada.esporte.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import br.gov.quixada.esporte.common.error.GlobalExceptionHandler;
import br.gov.quixada.esporte.config.ApiVersioningConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.accept.DefaultApiVersionStrategy;
import org.springframework.web.accept.PathApiVersionResolver;
import org.springframework.web.accept.SemanticApiVersionParser;

/**
 * Infraestrutura compartilhada dos testes de controller: MockMvc standalone com
 * o {@link GlobalExceptionHandler} e o versionamento de API, além de um
 * {@link Clock} fixo para tornar determinística a idade calculada.
 */
public final class ControllerTestSupport {

    public static final Instant FIXED_INSTANT = Instant.parse("2026-06-15T12:00:00Z");
    public static final ZoneId FIXED_ZONE = ZoneId.of("America/Fortaleza");
    public static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, FIXED_ZONE);

    private ControllerTestSupport() {
    }

    public static MockMvc mockMvc(Object... controllers) {
        return MockMvcBuilders.standaloneSetup(controllers)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setApiVersionStrategy(apiVersionStrategy())
                .build();
    }

    public static DefaultApiVersionStrategy apiVersionStrategy() {
        var strategy = new DefaultApiVersionStrategy(
                List.of(new PathApiVersionResolver(0, ApiVersioningConfig.versionedPath())),
                new SemanticApiVersionParser(),
                false,
                ApiVersioningConfig.CURRENT_VERSION,
                true,
                null,
                null);
        strategy.addSupportedVersion(ApiVersioningConfig.CURRENT_VERSION);
        return strategy;
    }
}
