package br.gov.quixada.sports.competition;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import br.gov.quixada.sports.competition.dto.CompetitionResponse;
import br.gov.quixada.sports.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompetitionController — free-form notes")
class CompetitionControllerTest {

    @Mock
    private CompetitionService service;

    @Mock
    private CompetitionMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new CompetitionController(service, mapper);
        mockMvc = ControllerTestSupport.mockMvc(controller);
    }

    private Competition aCompetition() {
        return Competition.builder()
                .id(1L)
                .name("Test Cup")
                .year(2026)
                .registrationStartDate(LocalDate.of(2026, 1, 1))
                .registrationEndDate(LocalDate.of(2026, 2, 1))
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(CompetitionStatus.IN_PROGRESS)
                .build();
    }

    private CompetitionResponse aResponse(String notes) {
        return new CompetitionResponse(1L, "Test Cup", 2026, 10L, "Soccer",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 31),
                CompetitionStatus.IN_PROGRESS, notes, null);
    }

    @Test
    @DisplayName("should save the provided notes")
    void shouldSaveNotes() throws Exception {
        var competition = aCompetition();
        when(service.updateNotes(1L, "End extended due to rain")).thenReturn(competition);
        when(mapper.toResponse(competition)).thenReturn(aResponse("End extended due to rain"));

        mockMvc.perform(patch("/v1/competitions/1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"End extended due to rain\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("End extended due to rain"));

        verify(service).updateNotes(1L, "End extended due to rain");
    }

    @Test
    @DisplayName("should clear the notes when the body does not include the field")
    void shouldClearNotes() throws Exception {
        var competition = aCompetition();
        when(service.updateNotes(1L, null)).thenReturn(competition);
        when(mapper.toResponse(competition)).thenReturn(aResponse(null));

        mockMvc.perform(patch("/v1/competitions/1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").doesNotExist());

        verify(service).updateNotes(1L, null);
    }

    @Test
    @DisplayName("should reject notes longer than 500 characters")
    void shouldRejectNotesLongerThan500Characters() throws Exception {
        var text = "a".repeat(501);

        mockMvc.perform(patch("/v1/competitions/1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"" + text + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("notes"));

        verifyNoInteractions(service);
    }
}
