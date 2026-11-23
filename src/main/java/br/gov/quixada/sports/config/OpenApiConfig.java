package br.gov.quixada.sports.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI sportsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sports Management API")
                        .version("1.0")
                        .description("REST API for managing sports competitions, registrations, matches, standings, and access control."))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    @Bean
    public OpenApiCustomizer v1PathNormalizer() {
        return openApi -> {
            Paths paths = openApi.getPaths();
            if (paths == null) {
                return;
            }

            Paths normalizedPaths = new Paths();
            paths.forEach((pathKey, pathItem) -> {
                String normalizedKey = pathKey;
                if (normalizedKey.startsWith("/v{version}")) {
                    normalizedKey = "/v1" + normalizedKey.substring("/v{version}".length());
                } else if (normalizedKey.startsWith("/v1.0")) {
                    normalizedKey = "/v1" + normalizedKey.substring("/v1.0".length());
                }

                cleanVersionParameters(pathItem);
                normalizedPaths.addPathItem(normalizedKey, pathItem);
            });

            openApi.setPaths(normalizedPaths);
        };
    }

    private void cleanVersionParameters(PathItem pathItem) {
        if (pathItem.getParameters() != null) {
            pathItem.getParameters().removeIf(this::isVersionPathParameter);
        }

        pathItem.readOperations().forEach(operation -> {
            if (operation.getParameters() != null) {
                operation.getParameters().removeIf(this::isVersionPathParameter);
            }
        });
    }

    private boolean isVersionPathParameter(Parameter parameter) {
        return "path".equalsIgnoreCase(parameter.getIn()) && "version".equals(parameter.getName());
    }
}
