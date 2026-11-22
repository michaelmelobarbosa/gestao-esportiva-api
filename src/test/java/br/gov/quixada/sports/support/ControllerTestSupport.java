package br.gov.quixada.sports.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import br.gov.quixada.sports.common.error.GlobalExceptionHandler;
import br.gov.quixada.sports.config.ApiVersioningConfig;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import org.springframework.web.accept.DefaultApiVersionStrategy;
import org.springframework.web.accept.PathApiVersionResolver;
import org.springframework.web.accept.SemanticApiVersionParser;

/**
 * Shared infrastructure for the controller tests: standalone MockMvc with the
 * {@link GlobalExceptionHandler}, pagination/principal resolvers, and API versioning,
 * plus a fixed {@link Clock} to make the computed age deterministic.
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
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver())
                .setApiVersionStrategy(apiVersionStrategy())
                .build();
    }

    /**
     * Same as {@link #mockMvc(Object...)}, but wraps each controller in the AOP proxy that
     * {@code @Validated} produces in the running application. Standalone MockMvc has no
     * {@code MethodValidationPostProcessor}, and Spring MVC skips its built-in method
     * validation when the controller type is annotated with {@code @Validated}; without
     * the proxy, constraint annotations on method parameters (e.g. {@code @Positive} on a
     * {@code @PathVariable}) would be silently ignored.
     */
    public static MockMvc mockMvcValidated(Object... controllers) {
        var proxied = Arrays.stream(controllers).map(ControllerTestSupport::validatedProxy).toArray();
        return mockMvc(proxied);
    }

    private static Object validatedProxy(Object controller) {
        var factory = new ProxyFactory(controller);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new MethodValidationInterceptor());
        return factory.getProxy();
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
