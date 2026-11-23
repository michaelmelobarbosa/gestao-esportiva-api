package br.gov.quixada.sports.match;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import br.gov.quixada.sports.match.dto.MatchScheduleGenerateRequest;
import br.gov.quixada.sports.match.dto.MatchSummaryResponse;
import br.gov.quixada.sports.match.exception.MatchScheduleAlreadyExistsException;
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
@DisplayName("MatchScheduleController")
class MatchScheduleControllerTest {

    @Mock
    private MatchService service;

    @Mock
    private MatchMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new MatchScheduleController(service, mapper);
        mockMvc = ControllerTestSupport.mockMvcValidated(controller);
    }

    private Match match(Long id, int round, LocalDateTime scheduledAt, String venue) {
        return Match.builder()
                .id(id)
                .round(round)
                .scheduledAt(scheduledAt)
                .venue(venue)
                .status(MatchStatus.SCHEDULED)
                .build();
    }

    private MatchSummaryResponse summary(Long id, int round, LocalDateTime scheduledAt, String venue) {
        return new MatchSummaryResponse(
                id, round, scheduledAt, venue,
                MatchStatus.SCHEDULED, null, null,
                100L, 200L, 101L, "Lions FC", 102L, "Eagles FC"
        );
    }

    @Test
    @DisplayName("should return 201 with generated schedule summary for a valid request body")
    void shouldReturn201ForValidPost() throws Exception {
        var r1 = LocalDateTime.of(2026, 7, 1, 19, 0);
        var r2 = LocalDateTime.of(2026, 7, 8, 19, 0);
        var m1 = match(1L, 1, r1, "Main Arena");
        var m2 = match(2L, 2, r2, "Main Arena");
        var expectedRequest = new MatchScheduleGenerateRequest(true, r1, 7, "Main Arena");

        when(service.generateSchedule(100L, 200L, expectedRequest)).thenReturn(List.of(m1, m2));
        when(mapper.toSummary(m1)).thenReturn(summary(1L, 1, r1, "Main Arena"));
        when(mapper.toSummary(m2)).thenReturn(summary(2L, 2, r2, "Main Arena"));

        mockMvc.perform(post("/v1/competitions/100/categories/200/matches/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "doubleRoundRobin": true,
                                  "firstRoundAt": "2026-07-01T19:00:00",
                                  "daysBetweenRounds": 7,
                                  "defaultVenue": "Main Arena"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRounds").value(2))
                .andExpect(jsonPath("$.totalMatches").value(2))
                .andExpect(jsonPath("$.matches[0].id").value(1))
                .andExpect(jsonPath("$.matches[0].round").value(1))
                .andExpect(jsonPath("$.matches[1].id").value(2))
                .andExpect(jsonPath("$.matches[1].round").value(2));

        verify(service).generateSchedule(100L, 200L, expectedRequest);
    }

    @Test
    @DisplayName("should use default request values when body is omitted and return 201")
    void shouldUseDefaultsWhenBodyEmpty() throws Exception {
        var m1 = match(1L, 1, null, null);
        var defaultRequest = new MatchScheduleGenerateRequest(false, null, null, null);

        when(service.generateSchedule(100L, 200L, defaultRequest)).thenReturn(List.of(m1));
        when(mapper.toSummary(m1)).thenReturn(summary(1L, 1, null, null));

        mockMvc.perform(post("/v1/competitions/100/categories/200/matches/generate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRounds").value(1))
                .andExpect(jsonPath("$.totalMatches").value(1))
                .andExpect(jsonPath("$.matches[0].id").value(1));

        verify(service).generateSchedule(eq(100L), eq(200L), eq(defaultRequest));
    }

    @Test
    @DisplayName("should return 400 when competitionId or categoryId is non-positive")
    void shouldReturn400ForInvalidPathId() throws Exception {
        mockMvc.perform(post("/v1/competitions/0/categories/200/matches/generate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", containsString("competitionId")));

        mockMvc.perform(post("/v1/competitions/100/categories/-3/matches/generate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", containsString("categoryId")));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("should return 400 when daysBetweenRounds is negative")
    void shouldReturn400WhenDaysBetweenRoundsNegative() throws Exception {
        mockMvc.perform(post("/v1/competitions/100/categories/200/matches/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "doubleRoundRobin": false,
                                  "daysBetweenRounds": -2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("daysBetweenRounds"));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("should return 409 when a match schedule already exists for the category")
    void shouldReturn409WhenScheduleAlreadyExists() throws Exception {
        when(service.generateSchedule(eq(100L), eq(200L), any(MatchScheduleGenerateRequest.class)))
                .thenThrow(new MatchScheduleAlreadyExistsException("Schedule already exists for this category"));

        mockMvc.perform(post("/v1/competitions/100/categories/200/matches/generate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Schedule already exists for this category"));
    }

    @Test
    @DisplayName("should reject an unsupported version with 400")
    void shouldRejectUnsupportedVersion() throws Exception {
        mockMvc.perform(post("/v9/competitions/100/categories/200/matches/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
