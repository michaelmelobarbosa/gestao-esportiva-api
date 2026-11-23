package br.gov.quixada.sports.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("OpenAPI / Swagger UI")
class OpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /v3/api-docs should return OpenAPI spec with normalized /v1 paths and security scheme")
    void shouldReturnOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/v1/athletes']").exists())
                .andExpect(jsonPath("$.paths['/v1/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/v1/auth/refresh']").exists())
                .andExpect(jsonPath("$.paths['/v1/auth/logout']").exists())
                .andExpect(jsonPath("$.paths['/v1/competitions/{competitionId}/categories/{categoryId}/matches/generate']").exists())
                .andExpect(jsonPath("$.paths['/v1/users/audit/export']").exists())
                .andExpect(content().string(not(containsString("/v{version}/"))));
    }

    @Test
    @DisplayName("GET /swagger-ui/index.html should return 200 OK")
    void shouldReturnSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
