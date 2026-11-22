package br.gov.quixada.sports.match;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import br.gov.quixada.sports.category.exception.CategoryNotFoundException;
import br.gov.quixada.sports.competition.exception.CompetitionNotFoundException;
import br.gov.quixada.sports.match.dto.StandingsResponse;
import br.gov.quixada.sports.registration.exception.InvalidRegistrationException;
import br.gov.quixada.sports.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@DisplayName("StandingsController")
class StandingsControllerTest {

    @Mock
    private StandingsService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new StandingsController(service);
        mockMvc = ControllerTestSupport.mockMvcValidated(controller);
    }

    @Test
    @DisplayName("should return 200 with ordered standings table")
    void shouldReturn200WithStandings() throws Exception {
        var first = new StandingsResponse(1, 10L, 101L, "Lions FC", 6, 2, 2, 0, 0, 5, 1, 4, 100.0);
        var second = new StandingsResponse(2, 20L, 102L, "Eagles FC", 0, 2, 0, 0, 2, 1, 5, -4, 0.0);
        when(service.calculate(100L, 200L)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/v1/competitions/100/categories/200/standings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].teamName").value("Lions FC"))
                .andExpect(jsonPath("$[0].points").value(6))
                .andExpect(jsonPath("$[1].position").value(2))
                .andExpect(jsonPath("$[1].teamName").value("Eagles FC"));

        verify(service).calculate(100L, 200L);
    }

    @Test
    @DisplayName("should return 400 when competitionId is non-positive")
    void shouldReturn400WhenCompetitionIdInvalid() throws Exception {
        mockMvc.perform(get("/v1/competitions/0/categories/200/standings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", containsString("competitionId")));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("should return 400 when categoryId is non-positive")
    void shouldReturn400WhenCategoryIdInvalid() throws Exception {
        mockMvc.perform(get("/v1/competitions/100/categories/-1/standings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", containsString("categoryId")));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("should return 404 when competition does not exist")
    void shouldReturn404WhenCompetitionNotFound() throws Exception {
        when(service.calculate(999L, 200L))
                .thenThrow(new CompetitionNotFoundException("Competition not found"));

        mockMvc.perform(get("/v1/competitions/999/categories/200/standings"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Competition not found"));
    }

    @Test
    @DisplayName("should return 404 when category does not exist")
    void shouldReturn404WhenCategoryNotFound() throws Exception {
        when(service.calculate(100L, 999L))
                .thenThrow(new CategoryNotFoundException("Category not found"));

        mockMvc.perform(get("/v1/competitions/100/categories/999/standings"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    @DisplayName("should return 400 when category does not belong to competition")
    void shouldReturn400WhenCategoryOutsideCompetition() throws Exception {
        when(service.calculate(100L, 200L))
                .thenThrow(new InvalidRegistrationException("Category does not belong to the informed competition"));

        mockMvc.perform(get("/v1/competitions/100/categories/200/standings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Category does not belong to the informed competition"));
    }

    @Test
    @DisplayName("should reject an unsupported version with 400")
    void shouldRejectUnsupportedVersion() throws Exception {
        mockMvc.perform(get("/v9/competitions/100/categories/200/standings"))
                .andExpect(status().isBadRequest());
    }
}
