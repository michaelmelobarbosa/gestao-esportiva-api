package br.gov.quixada.sports.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.RequestPath;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.function.Predicate;

@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    public static final String CURRENT_VERSION = "1.0";

    private static final String VERSIONED_PATH = "^/v\\d+(/.*)?$";
    private static final String[] NON_VERSIONED_PREFIXES = {"/v3/api-docs", "/swagger-ui"};

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.usePathSegment(0, versionedPath())
                .addSupportedVersions(CURRENT_VERSION)
                .setDefaultVersion(CURRENT_VERSION);
    }

    public static Predicate<RequestPath> versionedPath() {
        return path -> {
            var value = path.pathWithinApplication().value();
            for (var prefix : NON_VERSIONED_PREFIXES) {
                if (value.startsWith(prefix)) {
                    return false;
                }
            }
            return value.matches(VERSIONED_PATH);
        };
    }
}
