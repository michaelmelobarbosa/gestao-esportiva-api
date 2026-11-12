package br.gov.quixada.sports.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CORS")
class CorsConfigTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("should accept preflight from a configured origin")
    void shouldAcceptConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/v1/athletes")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    @DisplayName("should reject preflight from an unknown origin")
    void shouldRejectUnknownOrigin() throws Exception {
        mockMvc.perform(options("/v1/athletes")
                        .header(HttpHeaders.ORIGIN, "https://invasor.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
