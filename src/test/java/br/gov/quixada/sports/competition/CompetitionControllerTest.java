package br.gov.quixada.sports.competition;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import br.gov.quixada.sports.competition.dto.CompetitionCreateRequest;
import br.gov.quixada.sports.competition.dto.CompetitionResponse;
import br.gov.quixada.sports.competition.dto.CompetitionSummaryResponse;
import br.gov.quixada.sports.competition.dto.CompetitionUpdateRequest;
import br.gov.quixada.sports.competition.exception.CompetitionNotEditableException;
import br.gov.quixada.sports.competition.exception.CompetitionNotFoundException;
import br.gov.quixada.sports.competition.exception.CompetitionStatusTransitionException;
import br.gov.quixada.sports.competition.exception.InvalidPeriodException;
import br.gov.quixada.sports.modality.exception.InactiveModalityException;
import br.gov.quixada.sports.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompetitionController")
class CompetitionControllerTest {

    private static final String VALID_CREATE_JSON = """
            {
              "name": "Test Cup",
              "year": 2026,
              "modalityId": 10,
              "registrationStartDate": "2026-01-01",
              "registrationEndDate": "2026-02-01",
              "startDate": "2026-03-01",
              "endDate": "2026-12-31",
              "notes": "Opening tournament"
            }
            """;

    private static final String VALID_UPDATE_JSON = """
            {
              "name": "Updated Cup",
              "year": 2026,
              "registrationStartDate": "2026-01-05",
              "registrationEndDate": "2026-02-10",
              "startDate": "2026-03-01",
              "endDate": "2026-12-31"
            }
            """;

    @Mock
    private CompetitionService service;

    @Mock
    private CompetitionMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new CompetitionController(service, mapper);
        mockMvc = ControllerTestSupport.mockMvcValidated(controller);
    }

    private Competition aCompetition(CompetitionStatus status) {
        return Competition.builder()
                .id(1L)
                .name("Test Cup")
                .year(2026)
                .registrationStartDate(LocalDate.of(2026, 1, 1))
                .registrationEndDate(LocalDate.of(2026, 2, 1))
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(status)
                .build();
    }

    private CompetitionResponse aResponse(CompetitionStatus status, String notes) {
        return new CompetitionResponse(1L, "Test Cup", 2026, 10L, "Soccer",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 31),
                status, notes, null);
    }

    private CompetitionSummaryResponse aSummary(CompetitionStatus status) {
        return new CompetitionSummaryResponse(1L, "Test Cup", 2026, 10L, "Soccer",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 31),
                status);
    }

    @Nested
    @DisplayName("POST /v1/competitions")
    class Save {

        @Test
        @DisplayName("should return 201 with a Location header for a valid payload")
        void shouldReturn201WithLocation() throws Exception {
            var competition = aCompetition(CompetitionStatus.PLANNING);
            when(mapper.toEntity(any(CompetitionCreateRequest.class))).thenReturn(competition);
            when(service.save(same(competition), eq(10L))).thenReturn(competition);
            when(mapper.toResponse(competition))
                    .thenReturn(aResponse(CompetitionStatus.PLANNING, "Opening tournament"));

            mockMvc.perform(post("/v1/competitions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/v1/competitions/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.modalityId").value(10))
                    .andExpect(jsonPath("$.status").value("PLANNING"))
                    .andExpect(jsonPath("$.notes").value("Opening tournament"));

            verify(service).save(competition, 10L);
        }

        @Test
        @DisplayName("should return 400 when the name is blank")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            mockMvc.perform(post("/v1/competitions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON.replace("\"Test Cup\"", "\"\"")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("name"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 400 when the year is below 1900")
        void shouldReturn400WhenYearIsBelowMinimum() throws Exception {
            mockMvc.perform(post("/v1/competitions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON.replace("2026", "1899")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("year"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 400 when the period is invalid")
        void shouldReturn400WhenPeriodIsInvalid() throws Exception {
            var competition = aCompetition(CompetitionStatus.PLANNING);
            when(mapper.toEntity(any(CompetitionCreateRequest.class))).thenReturn(competition);
            when(service.save(same(competition), eq(10L)))
                    .thenThrow(new InvalidPeriodException("Start date cannot be after end date"));

            mockMvc.perform(post("/v1/competitions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Start date cannot be after end date"));
        }

        @Test
        @DisplayName("should return 409 when the modality is inactive")
        void shouldReturn409WhenModalityIsInactive() throws Exception {
            var competition = aCompetition(CompetitionStatus.PLANNING);
            when(mapper.toEntity(any(CompetitionCreateRequest.class))).thenReturn(competition);
            when(service.save(same(competition), eq(10L)))
                    .thenThrow(new InactiveModalityException("Modality is inactive"));

            mockMvc.perform(post("/v1/competitions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Modality is inactive"));
        }
    }

    @Nested
    @DisplayName("GET /v1/competitions")
    class Find {

        @Test
        @DisplayName("should return 200 with paginated summary list")
        void shouldReturn200WithPage() throws Exception {
            var competition = aCompetition(CompetitionStatus.IN_PROGRESS);
            when(service.findAll(eq("Cup"), eq(CompetitionStatus.IN_PROGRESS), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(competition), PageRequest.of(0, 10), 1));
            when(mapper.toSummary(competition)).thenReturn(aSummary(CompetitionStatus.IN_PROGRESS));

            mockMvc.perform(get("/v1/competitions")
                            .param("name", "Cup")
                            .param("status", "IN_PROGRESS")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Test Cup"))
                    .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("should return 200 by id")
        void shouldReturn200ById() throws Exception {
            var competition = aCompetition(CompetitionStatus.IN_PROGRESS);
            when(service.findByIdOrThrowNotFound(1L)).thenReturn(competition);
            when(mapper.toResponse(competition)).thenReturn(aResponse(CompetitionStatus.IN_PROGRESS, null));

            mockMvc.perform(get("/v1/competitions/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.modalityName").value("Soccer"));
        }

        @Test
        @DisplayName("should return 404 when the competition does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            when(service.findByIdOrThrowNotFound(99L))
                    .thenThrow(new CompetitionNotFoundException("Competition not found"));

            mockMvc.perform(get("/v1/competitions/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Competition not found"));
        }

        @Test
        @DisplayName("should return 400 for a non-positive id")
        void shouldReturn400ForNegativeId() throws Exception {
            mockMvc.perform(get("/v1/competitions/0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field", containsString("id")));

            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("PUT /v1/competitions/{id}")
    class Update {

        @Test
        @DisplayName("should return 200 and delegate to the service")
        void shouldReturn200() throws Exception {
            var competition = aCompetition(CompetitionStatus.PLANNING);
            when(mapper.toEntity(any(CompetitionUpdateRequest.class))).thenReturn(competition);
            when(service.update(eq(1L), same(competition))).thenReturn(competition);
            when(mapper.toResponse(competition)).thenReturn(aResponse(CompetitionStatus.PLANNING, null));

            mockMvc.perform(put("/v1/competitions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_UPDATE_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(service).update(1L, competition);
        }

        @Test
        @DisplayName("should return 409 when the competition cannot be edited")
        void shouldReturn409WhenNotEditable() throws Exception {
            var competition = aCompetition(CompetitionStatus.CANCELED);
            when(mapper.toEntity(any(CompetitionUpdateRequest.class))).thenReturn(competition);
            when(service.update(eq(1L), same(competition)))
                    .thenThrow(new CompetitionNotEditableException("Canceled competition cannot be edited"));

            mockMvc.perform(put("/v1/competitions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_UPDATE_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Canceled competition cannot be edited"));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/competitions/{id}/status")
    class ChangeStatus {

        @Test
        @DisplayName("should cancel the competition and return 200")
        void shouldCancelAndReturn200() throws Exception {
            var competition = aCompetition(CompetitionStatus.CANCELED);
            when(service.changeStatus(1L, CompetitionStatus.CANCELED)).thenReturn(competition);
            when(mapper.toResponse(competition)).thenReturn(aResponse(CompetitionStatus.CANCELED, null));

            mockMvc.perform(patch("/v1/competitions/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"CANCELED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELED"));

            verify(service).changeStatus(1L, CompetitionStatus.CANCELED);
        }

        @Test
        @DisplayName("should return 400 when the status is missing")
        void shouldReturn400WhenStatusIsMissing() throws Exception {
            mockMvc.perform(patch("/v1/competitions/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("status"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 409 for an invalid status transition")
        void shouldReturn409OnInvalidTransition() throws Exception {
            when(service.changeStatus(1L, CompetitionStatus.IN_PROGRESS))
                    .thenThrow(new CompetitionStatusTransitionException("Only CANCELED can be set manually"));

            mockMvc.perform(patch("/v1/competitions/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"IN_PROGRESS\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Only CANCELED can be set manually"));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/competitions/{id}/notes")
    class UpdateNotes {

        @Test
        @DisplayName("should save the provided notes")
        void shouldSaveNotes() throws Exception {
            var competition = aCompetition(CompetitionStatus.IN_PROGRESS);
            when(service.updateNotes(1L, "End extended due to rain")).thenReturn(competition);
            when(mapper.toResponse(competition))
                    .thenReturn(aResponse(CompetitionStatus.IN_PROGRESS, "End extended due to rain"));

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
            var competition = aCompetition(CompetitionStatus.IN_PROGRESS);
            when(service.updateNotes(1L, null)).thenReturn(competition);
            when(mapper.toResponse(competition)).thenReturn(aResponse(CompetitionStatus.IN_PROGRESS, null));

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

    @Nested
    @DisplayName("versioning")
    class Versioning {

        @Test
        @DisplayName("should reject an unsupported version with 400")
        void shouldRejectUnsupportedVersion() throws Exception {
            mockMvc.perform(get("/v9/competitions/1"))
                    .andExpect(status().isBadRequest());
        }
    }
}
