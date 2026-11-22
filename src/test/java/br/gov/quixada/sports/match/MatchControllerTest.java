package br.gov.quixada.sports.match;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import java.time.LocalDateTime;
import java.util.List;

import br.gov.quixada.sports.match.dto.MatchCreateRequest;
import br.gov.quixada.sports.match.dto.MatchResponse;
import br.gov.quixada.sports.match.dto.MatchResultRequest;
import br.gov.quixada.sports.match.dto.MatchSummaryResponse;
import br.gov.quixada.sports.match.dto.MatchUpdateRequest;
import br.gov.quixada.sports.match.exception.InvalidMatchException;
import br.gov.quixada.sports.match.exception.MatchNotFoundException;
import br.gov.quixada.sports.match.exception.MatchStatusTransitionException;
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
@DisplayName("MatchController")
class MatchControllerTest {

    private static final String VALID_CREATE_JSON = """
            {
              "homeRegistrationId": 10,
              "awayRegistrationId": 20,
              "round": 1,
              "scheduledAt": "2026-06-20T19:00:00",
              "venue": "Municipal Arena",
              "notes": "Opening match"
            }
            """;

    private static final String VALID_UPDATE_JSON = """
            {
              "homeRegistrationId": 10,
              "awayRegistrationId": 20,
              "round": 2,
              "scheduledAt": "2026-06-21T20:00:00",
              "venue": "Secondary Gym"
            }
            """;

    @Mock
    private MatchService service;

    @Mock
    private MatchMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new MatchController(service, mapper);
        mockMvc = ControllerTestSupport.mockMvcValidated(controller);
    }

    private Match aMatch(MatchStatus status) {
        return Match.builder()
                .id(1L)
                .round(1)
                .scheduledAt(LocalDateTime.of(2026, 6, 20, 19, 0))
                .venue("Municipal Arena")
                .status(status)
                .build();
    }

    private MatchResponse aResponse(MatchStatus status, Integer homeGoals, Integer awayGoals, String notes) {
        return new MatchResponse(
                1L, 1, LocalDateTime.of(2026, 6, 20, 19, 0), "Municipal Arena",
                status, homeGoals, awayGoals,
                100L, "Municipal Cup", 200L, "U-20",
                10L, 101L, "Lions FC",
                20L, 102L, "Eagles FC",
                notes, null
        );
    }

    private MatchSummaryResponse aSummary(MatchStatus status) {
        return new MatchSummaryResponse(
                1L, 1, LocalDateTime.of(2026, 6, 20, 19, 0), "Municipal Arena",
                status, null, null,
                100L, 200L, 101L, "Lions FC", 102L, "Eagles FC"
        );
    }

    @Nested
    @DisplayName("POST /v1/matches")
    class Save {

        @Test
        @DisplayName("should return 201 with Location header for a valid payload")
        void shouldReturn201WithLocation() throws Exception {
            var match = aMatch(MatchStatus.SCHEDULED);
            when(service.create(any(MatchCreateRequest.class))).thenReturn(match);
            when(mapper.toResponse(match)).thenReturn(aResponse(MatchStatus.SCHEDULED, null, null, "Opening match"));

            mockMvc.perform(post("/v1/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/v1/matches/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("SCHEDULED"))
                    .andExpect(jsonPath("$.homeTeamName").value("Lions FC"));

            verify(service).create(any(MatchCreateRequest.class));
        }

        @Test
        @DisplayName("should return 400 when round is non-positive")
        void shouldReturn400WhenRoundIsZero() throws Exception {
            mockMvc.perform(post("/v1/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON.replace("\"round\": 1", "\"round\": 0")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("round"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 400 when business validation fails")
        void shouldReturn400OnInvalidMatch() throws Exception {
            when(service.create(any(MatchCreateRequest.class)))
                    .thenThrow(new InvalidMatchException("Home and away teams must be different"));

            mockMvc.perform(post("/v1/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Home and away teams must be different"));
        }
    }

    @Nested
    @DisplayName("GET /v1/matches")
    class Find {

        @Test
        @DisplayName("should return 200 with filtered page of matches")
        void shouldReturn200WithPage() throws Exception {
            var match = aMatch(MatchStatus.SCHEDULED);
            when(service.findAll(eq(100L), eq(200L), eq(MatchStatus.SCHEDULED), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(match), PageRequest.of(0, 10), 1));
            when(mapper.toSummary(match)).thenReturn(aSummary(MatchStatus.SCHEDULED));

            mockMvc.perform(get("/v1/matches")
                            .param("competitionId", "100")
                            .param("categoryId", "200")
                            .param("status", "SCHEDULED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].homeTeamName").value("Lions FC"));
        }

        @Test
        @DisplayName("should return 200 by id")
        void shouldReturn200ById() throws Exception {
            var match = aMatch(MatchStatus.SCHEDULED);
            when(service.findByIdOrThrowNotFound(1L)).thenReturn(match);
            when(mapper.toResponse(match)).thenReturn(aResponse(MatchStatus.SCHEDULED, null, null, null));

            mockMvc.perform(get("/v1/matches/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.venue").value("Municipal Arena"));
        }

        @Test
        @DisplayName("should return 404 when the match is not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(service.findByIdOrThrowNotFound(99L))
                    .thenThrow(new MatchNotFoundException("Match not found"));

            mockMvc.perform(get("/v1/matches/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Match not found"));
        }

        @Test
        @DisplayName("should return 400 for a non-positive id")
        void shouldReturn400ForNegativeId() throws Exception {
            mockMvc.perform(get("/v1/matches/-5"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field", containsString("id")));

            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("PUT /v1/matches/{id}")
    class Update {

        @Test
        @DisplayName("should return 200 and delegate to the service")
        void shouldReturn200() throws Exception {
            var match = aMatch(MatchStatus.SCHEDULED);
            when(service.update(eq(1L), any(MatchUpdateRequest.class))).thenReturn(match);
            when(mapper.toResponse(match)).thenReturn(aResponse(MatchStatus.SCHEDULED, null, null, null));

            mockMvc.perform(put("/v1/matches/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_UPDATE_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(service).update(eq(1L), any(MatchUpdateRequest.class));
        }

        @Test
        @DisplayName("should return 400 when editing a match that is not SCHEDULED")
        void shouldReturn400WhenNotScheduled() throws Exception {
            when(service.update(eq(1L), any(MatchUpdateRequest.class)))
                    .thenThrow(new InvalidMatchException("Only matches with SCHEDULED status can be edited"));

            mockMvc.perform(put("/v1/matches/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_UPDATE_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Only matches with SCHEDULED status can be edited"));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/matches/{id}/status")
    class ChangeStatus {

        @Test
        @DisplayName("should transition status and return 200")
        void shouldChangeStatus() throws Exception {
            var match = aMatch(MatchStatus.IN_PROGRESS);
            when(service.changeStatus(1L, MatchStatus.IN_PROGRESS)).thenReturn(match);
            when(mapper.toResponse(match)).thenReturn(aResponse(MatchStatus.IN_PROGRESS, null, null, null));

            mockMvc.perform(patch("/v1/matches/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"IN_PROGRESS\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

            verify(service).changeStatus(1L, MatchStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("should return 400 when status is missing")
        void shouldReturn400WhenStatusMissing() throws Exception {
            mockMvc.perform(patch("/v1/matches/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("status"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 409 for an invalid status transition")
        void shouldReturn409OnInvalidTransition() throws Exception {
            when(service.changeStatus(1L, MatchStatus.SCHEDULED))
                    .thenThrow(new MatchStatusTransitionException("Invalid status transition"));

            mockMvc.perform(patch("/v1/matches/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"SCHEDULED\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Invalid status transition"));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/matches/{id}/result")
    class RecordResult {

        @Test
        @DisplayName("should record the match result and return 200")
        void shouldRecordResult() throws Exception {
            var match = aMatch(MatchStatus.COMPLETED);
            when(service.recordResult(eq(1L), any(MatchResultRequest.class))).thenReturn(match);
            when(mapper.toResponse(match)).thenReturn(aResponse(MatchStatus.COMPLETED, 3, 1, null));

            mockMvc.perform(patch("/v1/matches/1/result")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"homeGoals\":3,\"awayGoals\":1}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.homeGoals").value(3))
                    .andExpect(jsonPath("$.awayGoals").value(1));

            verify(service).recordResult(eq(1L), any(MatchResultRequest.class));
        }

        @Test
        @DisplayName("should return 400 when goals are negative")
        void shouldReturn400WhenGoalsNegative() throws Exception {
            mockMvc.perform(patch("/v1/matches/1/result")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"homeGoals\":-1,\"awayGoals\":0}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("homeGoals"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 400 when match is not IN_PROGRESS")
        void shouldReturn400WhenMatchNotInProgress() throws Exception {
            when(service.recordResult(eq(1L), any(MatchResultRequest.class)))
                    .thenThrow(new InvalidMatchException("Results can only be recorded for an IN_PROGRESS match"));

            mockMvc.perform(patch("/v1/matches/1/result")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"homeGoals\":2,\"awayGoals\":2}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Results can only be recorded for an IN_PROGRESS match"));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/matches/{id}/notes")
    class UpdateNotes {

        @Test
        @DisplayName("should save the provided notes")
        void shouldSaveNotes() throws Exception {
            var match = aMatch(MatchStatus.SCHEDULED);
            when(service.updateNotes(1L, "Delayed by 15 minutes")).thenReturn(match);
            when(mapper.toResponse(match)).thenReturn(aResponse(MatchStatus.SCHEDULED, null, null, "Delayed by 15 minutes"));

            mockMvc.perform(patch("/v1/matches/1/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"notes\":\"Delayed by 15 minutes\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notes").value("Delayed by 15 minutes"));

            verify(service).updateNotes(1L, "Delayed by 15 minutes");
        }

        @Test
        @DisplayName("should clear notes when omitted")
        void shouldClearNotes() throws Exception {
            var match = aMatch(MatchStatus.SCHEDULED);
            when(service.updateNotes(1L, null)).thenReturn(match);
            when(mapper.toResponse(match)).thenReturn(aResponse(MatchStatus.SCHEDULED, null, null, null));

            mockMvc.perform(patch("/v1/matches/1/notes")
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

            mockMvc.perform(patch("/v1/matches/1/notes")
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
            mockMvc.perform(get("/v9/matches/1"))
                    .andExpect(status().isBadRequest());
        }
    }
}
