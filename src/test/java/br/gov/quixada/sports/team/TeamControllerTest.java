package br.gov.quixada.sports.team;

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

import java.util.List;

import br.gov.quixada.sports.club.exception.InactiveClubException;
import br.gov.quixada.sports.common.error.ActiveReferenceException;
import br.gov.quixada.sports.support.ControllerTestSupport;
import br.gov.quixada.sports.team.dto.TeamCreateRequest;
import br.gov.quixada.sports.team.dto.TeamResponse;
import br.gov.quixada.sports.team.dto.TeamSummaryResponse;
import br.gov.quixada.sports.team.dto.TeamUpdateRequest;
import br.gov.quixada.sports.team.exception.InactiveTeamException;
import br.gov.quixada.sports.team.exception.TeamNotFoundException;
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
@DisplayName("TeamController")
class TeamControllerTest {

    private static final String VALID_CREATE_JSON = """
            {
              "name": "Lions FC",
              "clubId": 10,
              "modalityId": 20,
              "manager": "Roberto Silva"
            }
            """;

    private static final String VALID_UPDATE_JSON = """
            {
              "name": "Lions Futsal",
              "manager": "Roberto Silva"
            }
            """;

    @Mock
    private TeamService service;

    @Mock
    private TeamMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new TeamController(service, mapper);
        mockMvc = ControllerTestSupport.mockMvcValidated(controller);
    }

    private Team aTeam(TeamStatus status) {
        return Team.builder()
                .id(1L)
                .name("Lions FC")
                .manager("Roberto Silva")
                .status(status)
                .build();
    }

    private TeamResponse aResponse(TeamStatus status) {
        return new TeamResponse(1L, "Lions FC", 10L, "Quixadá Club", 20L, "Futsal", "Roberto Silva", status);
    }

    private TeamSummaryResponse aSummary(TeamStatus status) {
        return new TeamSummaryResponse(1L, "Lions FC", 10L, "Quixadá Club", 20L, "Futsal", "Roberto Silva", status);
    }

    @Nested
    @DisplayName("POST /v1/teams")
    class Save {

        @Test
        @DisplayName("should return 201 with Location header for a valid payload")
        void shouldReturn201WithLocation() throws Exception {
            var team = aTeam(TeamStatus.ACTIVE);
            when(mapper.toEntity(any(TeamCreateRequest.class))).thenReturn(team);
            when(service.save(same(team), eq(10L), eq(20L))).thenReturn(team);
            when(mapper.toResponse(team)).thenReturn(aResponse(TeamStatus.ACTIVE));

            mockMvc.perform(post("/v1/teams")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/v1/teams/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(service).save(team, 10L, 20L);
        }

        @Test
        @DisplayName("should return 400 when manager is blank")
        void shouldReturn400WhenManagerBlank() throws Exception {
            mockMvc.perform(post("/v1/teams")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON.replace("\"Roberto Silva\"", "\"\"")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("manager"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 409 when club is inactive")
        void shouldReturn409WhenClubInactive() throws Exception {
            var team = aTeam(TeamStatus.ACTIVE);
            when(mapper.toEntity(any(TeamCreateRequest.class))).thenReturn(team);
            when(service.save(same(team), eq(10L), eq(20L)))
                    .thenThrow(new InactiveClubException("Club is inactive"));

            mockMvc.perform(post("/v1/teams")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Club is inactive"));
        }
    }

    @Nested
    @DisplayName("GET /v1/teams")
    class Find {

        @Test
        @DisplayName("should return 200 with filtered page")
        void shouldReturn200WithPage() throws Exception {
            var team = aTeam(TeamStatus.ACTIVE);
            when(service.findAll(eq("Lions"), eq(TeamStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(team), PageRequest.of(0, 10), 1));
            when(mapper.toSummary(team)).thenReturn(aSummary(TeamStatus.ACTIVE));

            mockMvc.perform(get("/v1/teams")
                            .param("name", "Lions")
                            .param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].clubName").value("Quixadá Club"));
        }

        @Test
        @DisplayName("should return 200 by id")
        void shouldReturn200ById() throws Exception {
            var team = aTeam(TeamStatus.ACTIVE);
            when(service.findByIdOrThrowNotFound(1L)).thenReturn(team);
            when(mapper.toResponse(team)).thenReturn(aResponse(TeamStatus.ACTIVE));

            mockMvc.perform(get("/v1/teams/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.modalityName").value("Futsal"));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(service.findByIdOrThrowNotFound(99L))
                    .thenThrow(new TeamNotFoundException("Team not found"));

            mockMvc.perform(get("/v1/teams/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Team not found"));
        }

        @Test
        @DisplayName("should return 400 for a non-positive id")
        void shouldReturn400ForNegativeId() throws Exception {
            mockMvc.perform(get("/v1/teams/0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field", containsString("id")));

            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("PUT /v1/teams/{id}")
    class Update {

        @Test
        @DisplayName("should return 200 and delegate to service")
        void shouldReturn200() throws Exception {
            var team = aTeam(TeamStatus.ACTIVE);
            when(mapper.toEntity(any(TeamUpdateRequest.class))).thenReturn(team);
            when(service.update(eq(1L), same(team))).thenReturn(team);
            when(mapper.toResponse(team)).thenReturn(aResponse(TeamStatus.ACTIVE));

            mockMvc.perform(put("/v1/teams/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_UPDATE_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(service).update(1L, team);
        }

        @Test
        @DisplayName("should return 409 when team is inactive")
        void shouldReturn409WhenInactive() throws Exception {
            var team = aTeam(TeamStatus.INACTIVE);
            when(mapper.toEntity(any(TeamUpdateRequest.class))).thenReturn(team);
            when(service.update(eq(1L), same(team)))
                    .thenThrow(new InactiveTeamException("Team is inactive"));

            mockMvc.perform(put("/v1/teams/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_UPDATE_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Team is inactive"));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/teams/{id}/status")
    class ChangeStatus {

        @Test
        @DisplayName("should activate and return 200")
        void shouldActivate() throws Exception {
            var team = aTeam(TeamStatus.ACTIVE);
            when(service.activate(1L)).thenReturn(team);
            when(mapper.toResponse(team)).thenReturn(aResponse(TeamStatus.ACTIVE));

            mockMvc.perform(patch("/v1/teams/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"ACTIVE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(service).activate(1L);
        }

        @Test
        @DisplayName("should return 409 when deactivating with active registrations")
        void shouldReturn409OnActiveReference() throws Exception {
            when(service.deactivate(1L))
                    .thenThrow(new ActiveReferenceException("Team has active registrations"));

            mockMvc.perform(patch("/v1/teams/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"INACTIVE\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Team has active registrations"));
        }
    }

    @Nested
    @DisplayName("versioning")
    class Versioning {

        @Test
        @DisplayName("should reject an unsupported version with 400")
        void shouldRejectUnsupportedVersion() throws Exception {
            mockMvc.perform(get("/v9/teams/1"))
                    .andExpect(status().isBadRequest());
        }
    }
}
