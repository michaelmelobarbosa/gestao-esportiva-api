package br.gov.quixada.esporte.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.RequestPath;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiVersioningConfig")
class ApiVersioningConfigTest {

    @Mock
    private ApiVersionConfigurer configurer;

    @Test
    @DisplayName("deve configurar versão por segmento de path com v1 suportada e default")
    void deveConfigurarVersionamentoPorPath() {
        when(configurer.usePathSegment(eq(0), any())).thenReturn(configurer);
        when(configurer.addSupportedVersions(any(String[].class))).thenReturn(configurer);
        when(configurer.setDefaultVersion(anyString())).thenReturn(configurer);

        new ApiVersioningConfig().configureApiVersioning(configurer);

        var predicateCaptor = ArgumentCaptor.forClass(Predicate.class);
        verify(configurer).usePathSegment(eq(0), predicateCaptor.capture());
        verify(configurer).addSupportedVersions(ApiVersioningConfig.CURRENT_VERSION);
        verify(configurer).setDefaultVersion(ApiVersioningConfig.CURRENT_VERSION);

        @SuppressWarnings("unchecked")
        Predicate<RequestPath> versionedPath = predicateCaptor.getValue();

        assertThat(versionedPath.test(RequestPath.parse("/v1/atletas", ""))).isTrue();
        assertThat(versionedPath.test(RequestPath.parse("/v10/clubes", ""))).isTrue();
        assertThat(versionedPath.test(RequestPath.parse("/error", ""))).isFalse();
        assertThat(versionedPath.test(RequestPath.parse("/auth/login", ""))).isFalse();
        assertThat(versionedPath.test(RequestPath.parse("/v3/api-docs", ""))).isFalse();
        assertThat(versionedPath.test(RequestPath.parse("/swagger-ui/index.html", ""))).isFalse();
    }
}
