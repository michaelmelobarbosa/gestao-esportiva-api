package br.gov.quixada.sports.registration;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import br.gov.quixada.sports.registration.dto.TeamRegistrationCreateRequest;
import br.gov.quixada.sports.registration.dto.TeamRegistrationResponse;
import br.gov.quixada.sports.registration.dto.TeamRegistrationSummaryResponse;
import br.gov.quixada.sports.registration.exception.InvalidStatusTransitionException;
import br.gov.quixada.sports.registration.exception.RegistrationClosedException;
import br.gov.quixada.sports.registration.exception.TeamAlreadyRegisteredException;
import br.gov.quixada.sports.registration.exception.TeamRegistrationNotFoundException;
import br.gov.quixada.sports.registration.exception.TeamRegistrationWithoutAthletesException;
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
@DisplayName("TeamRegistrationController")
class TeamRegistrationControllerTest {

    private static final String VALID_CREATE_JSON = """
            {
              "competitionId": 100,
              "categoryId": 200,
              "teamId": 300,
              "notes": "Submitted with full documents"
            }
            """;

    @Mock
    private TeamRegistrationService service;

    @Mock
    private TeamRegistrationMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new TeamRegistrationController(service, mapper);
        mockMvc = ControllerTestSupport.mockMvcValidated(controller);
    }

    private TeamRegistration aRegistration(RegistrationStatus status) {
        return TeamRegistration.builder()
                .id(1L)
                .registrationDate(LocalDateTime.of(2026, 1, 15, 10, 0))
                .status(status)
                .build();
    }

    private TeamRegistrationResponse aResponse(RegistrationStatus status, String notes) {
        return new TeamRegistrationResponse(
                1L, 100L, "Municipal Cup", 200L, "U-20", 300L, "Lions FC",
                LocalDateTime.of(2026, 1, 15, 10, 0), status, notes, null
        );
    }

    private TeamRegistrationSummaryResponse aSummary(RegistrationStatus status) {
        return new TeamRegistrationSummaryResponse(
                1L, 100L, "Municipal Cup", 200L, "U-20", 300L, "Lions FC", status
        );
    }

    @Nested
    @DisplayName("POST /v1/team-registrations")
    class Save {

        @Test
        @DisplayName("should return 201 with Location header for a valid payload")
        void shouldReturn201WithLocation() throws Exception {
            var registration = aRegistration(RegistrationStatus.PENDING);
            when(mapper.toEntity(any(TeamRegistrationCreateRequest.class))).thenReturn(registration);
            when(service.save(same(registration), eq(100L), eq(200L), eq(300L))).thenReturn(registration);
            when(mapper.toResponse(registration))
                    .thenReturn(aResponse(RegistrationStatus.PENDING, "Submitted with full documents"));

            mockMvc.perform(post("/v1/team-registrations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/v1/team-registrations/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.teamName").value("Lions FC"));

            verify(service).save(registration, 100L, 200L, 300L);
        }

        @Test
        @DisplayName("should return 400 when teamId is missing")
        void shouldReturn400WhenTeamIdIsMissing() throws Exception {
            mockMvc.perform(post("/v1/team-registrations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"competitionId\":100,\"categoryId\":200}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("teamId"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 409 when team is already registered in competition")
        void shouldReturn409WhenTeamAlreadyRegistered() throws Exception {
            var registration = aRegistration(RegistrationStatus.PENDING);
            when(mapper.toEntity(any(TeamRegistrationCreateRequest.class))).thenReturn(registration);
            when(service.save(same(registration), eq(100L), eq(200L), eq(300L)))
                    .thenThrow(new TeamAlreadyRegisteredException("Team already has an active registration"));

            mockMvc.perform(post("/v1/team-registrations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Team already has an active registration"));
        }

        @Test
        @DisplayName("should return 409 when registration window is closed")
        void shouldReturn409WhenRegistrationClosed() throws Exception {
            var registration = aRegistration(RegistrationStatus.PENDING);
            when(mapper.toEntity(any(TeamRegistrationCreateRequest.class))).thenReturn(registration);
            when(service.save(same(registration), eq(100L), eq(200L), eq(300L)))
                    .thenThrow(new RegistrationClosedException("Registration window is closed"));

            mockMvc.perform(post("/v1/team-registrations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Registration window is closed"));
        }
    }

    @Nested
    @DisplayName("GET /v1/team-registrations")
    class Find {

        @Test
        @DisplayName("should return 200 with filtered page")
        void shouldReturn200WithPage() throws Exception {
            var registration = aRegistration(RegistrationStatus.APPROVED);
            when(service.findAll(eq(100L), eq(RegistrationStatus.APPROVED), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(registration), PageRequest.of(0, 10), 1));
            when(mapper.toSummary(registration)).thenReturn(aSummary(RegistrationStatus.APPROVED));

            mockMvc.perform(get("/v1/team-registrations")
                            .param("competitionId", "100")
                            .param("status", "APPROVED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].status").value("APPROVED"));
        }

        @Test
        @DisplayName("should return 200 by id")
        void shouldReturn200ById() throws Exception {
            var registration = aRegistration(RegistrationStatus.APPROVED);
            when(service.findByIdOrThrowNotFound(1L)).thenReturn(registration);
            when(mapper.toResponse(registration)).thenReturn(aResponse(RegistrationStatus.APPROVED, null));

            mockMvc.perform(get("/v1/team-registrations/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.competitionName").value("Municipal Cup"));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(service.findByIdOrThrowNotFound(99L))
                    .thenThrow(new TeamRegistrationNotFoundException("Team registration not found"));

            mockMvc.perform(get("/v1/team-registrations/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Team registration not found"));
        }

        @Test
        @DisplayName("should return 400 for a non-positive id")
        void shouldReturn400ForNegativeId() throws Exception {
            mockMvc.perform(get("/v1/team-registrations/0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field", containsString("id")));

            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("PATCH /v1/team-registrations/{id}/status")
    class ChangeStatus {

        @Test
        @DisplayName("should approve registration and return 200")
        void shouldApproveAndReturn200() throws Exception {
            var registration = aRegistration(RegistrationStatus.APPROVED);
            when(service.changeStatus(1L, RegistrationStatus.APPROVED)).thenReturn(registration);
            when(mapper.toResponse(registration)).thenReturn(aResponse(RegistrationStatus.APPROVED, null));

            mockMvc.perform(patch("/v1/team-registrations/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"APPROVED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));

            verify(service).changeStatus(1L, RegistrationStatus.APPROVED);
        }

        @Test
        @DisplayName("should return 400 when status is missing")
        void shouldReturn400WhenStatusMissing() throws Exception {
            mockMvc.perform(patch("/v1/team-registrations/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("status"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 409 when approving a team without athletes")
        void shouldReturn409WhenNoAthletes() throws Exception {
            when(service.changeStatus(1L, RegistrationStatus.APPROVED))
                    .thenThrow(new TeamRegistrationWithoutAthletesException("At least one active athlete is required"));

            mockMvc.perform(patch("/v1/team-registrations/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"APPROVED\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("At least one active athlete is required"));
        }

        @Test
        @DisplayName("should return 409 for an invalid status transition")
        void shouldReturn409OnInvalidTransition() throws Exception {
            when(service.changeStatus(1L, RegistrationStatus.PENDING))
                    .thenThrow(new InvalidStatusTransitionException("Invalid status transition"));

            mockMvc.perform(patch("/v1/team-registrations/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"PENDING\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Invalid status transition"));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/team-registrations/{id}/notes")
    class UpdateNotes {

        @Test
        @DisplayName("should save the provided notes")
        void shouldSaveNotes() throws Exception {
            var registration = aRegistration(RegistrationStatus.PENDING);
            when(service.updateNotes(1L, "Pending medical certificate")).thenReturn(registration);
            when(mapper.toResponse(registration))
                    .thenReturn(aResponse(RegistrationStatus.PENDING, "Pending medical certificate"));

            mockMvc.perform(patch("/v1/team-registrations/1/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"notes\":\"Pending medical certificate\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notes").value("Pending medical certificate"));

            verify(service).updateNotes(1L, "Pending medical certificate");
        }

        @Test
        @DisplayName("should clear notes when omitted")
        void shouldClearNotes() throws Exception {
            var registration = aRegistration(RegistrationStatus.PENDING);
            when(service.updateNotes(1L, null)).thenReturn(registration);
            when(mapper.toResponse(registration)).thenReturn(aResponse(RegistrationStatus.PENDING, null));

            mockMvc.perform(patch("/v1/team-registrations/1/notes")
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

            mockMvc.perform(patch("/v1/team-registrations/1/notes")
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
            mockMvc.perform(get("/v9/team-registrations/1"))
                    .andExpect(status().isBadRequest());
        }
    }
}
