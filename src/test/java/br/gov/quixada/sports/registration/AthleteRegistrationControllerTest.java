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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import br.gov.quixada.sports.registration.dto.AthleteRegistrationCreateRequest;
import br.gov.quixada.sports.registration.dto.AthleteRegistrationResponse;
import br.gov.quixada.sports.registration.dto.AthleteRegistrationSummaryResponse;
import br.gov.quixada.sports.registration.exception.AthleteAlreadyRegisteredException;
import br.gov.quixada.sports.registration.exception.AthleteOutsideCategoryException;
import br.gov.quixada.sports.registration.exception.AthleteRegistrationNotFoundException;
import br.gov.quixada.sports.registration.exception.JerseyNumberAlreadyUsedException;
import br.gov.quixada.sports.registration.exception.LastActiveAthleteException;
import br.gov.quixada.sports.registration.exception.RosterLimitExceededException;
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
@DisplayName("AthleteRegistrationController")
class AthleteRegistrationControllerTest {

    private static final String VALID_CREATE_JSON = """
            {
              "teamRegistrationId": 10,
              "athleteId": 50,
              "jerseyNumber": "10",
              "notes": "Team captain"
            }
            """;

    @Mock
    private AthleteRegistrationService service;

    @Mock
    private AthleteRegistrationMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new AthleteRegistrationController(service, mapper);
        mockMvc = ControllerTestSupport.mockMvcValidated(controller);
    }

    private AthleteRegistration aRegistration(RegistrationStatus status, String jerseyNumber) {
        return AthleteRegistration.builder()
                .id(1L)
                .jerseyNumber(jerseyNumber)
                .registrationDate(LocalDateTime.of(2026, 1, 20, 11, 0))
                .status(status)
                .build();
    }

    private AthleteRegistrationResponse aResponse(RegistrationStatus status, String jerseyNumber, String notes) {
        return new AthleteRegistrationResponse(
                1L, 10L, 50L, "Francisco da Silva", jerseyNumber,
                LocalDateTime.of(2026, 1, 20, 11, 0), status, notes, null
        );
    }

    private AthleteRegistrationSummaryResponse aSummary(RegistrationStatus status) {
        return new AthleteRegistrationSummaryResponse(
                1L, 10L, 50L, "Francisco da Silva", "10", status
        );
    }

    @Nested
    @DisplayName("POST /v1/athlete-registrations")
    class Save {

        @Test
        @DisplayName("should return 201 with Location header for a valid payload")
        void shouldReturn201WithLocation() throws Exception {
            var registration = aRegistration(RegistrationStatus.PENDING, "10");
            when(mapper.toEntity(any(AthleteRegistrationCreateRequest.class))).thenReturn(registration);
            when(service.save(same(registration), eq(10L), eq(50L))).thenReturn(registration);
            when(mapper.toResponse(registration))
                    .thenReturn(aResponse(RegistrationStatus.PENDING, "10", "Team captain"));

            mockMvc.perform(post("/v1/athlete-registrations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/v1/athlete-registrations/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.jerseyNumber").value("10"))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            verify(service).save(registration, 10L, 50L);
        }

        @Test
        @DisplayName("should return 400 when jerseyNumber contains non-digits")
        void shouldReturn400WhenJerseyNumberNonNumeric() throws Exception {
            mockMvc.perform(post("/v1/athlete-registrations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON.replace("\"10\"", "\"10A\"")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("jerseyNumber"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 400 when athlete age is outside category range")
        void shouldReturn400WhenAthleteOutsideCategory() throws Exception {
            var registration = aRegistration(RegistrationStatus.PENDING, "10");
            when(mapper.toEntity(any(AthleteRegistrationCreateRequest.class))).thenReturn(registration);
            when(service.save(same(registration), eq(10L), eq(50L)))
                    .thenThrow(new AthleteOutsideCategoryException("Athlete age is outside category bounds"));

            mockMvc.perform(post("/v1/athlete-registrations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Athlete age is outside category bounds"));
        }

        @Test
        @DisplayName("should return 409 when athlete is already registered in competition")
        void shouldReturn409WhenAthleteAlreadyRegistered() throws Exception {
            var registration = aRegistration(RegistrationStatus.PENDING, "10");
            when(mapper.toEntity(any(AthleteRegistrationCreateRequest.class))).thenReturn(registration);
            when(service.save(same(registration), eq(10L), eq(50L)))
                    .thenThrow(new AthleteAlreadyRegisteredException("Athlete already registered in this competition"));

            mockMvc.perform(post("/v1/athlete-registrations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Athlete already registered in this competition"));
        }

        @Test
        @DisplayName("should return 409 when category maxAthletes limit is exceeded")
        void shouldReturn409WhenRosterLimitExceeded() throws Exception {
            var registration = aRegistration(RegistrationStatus.PENDING, "10");
            when(mapper.toEntity(any(AthleteRegistrationCreateRequest.class))).thenReturn(registration);
            when(service.save(same(registration), eq(10L), eq(50L)))
                    .thenThrow(new RosterLimitExceededException("Maximum roster limit reached"));

            mockMvc.perform(post("/v1/athlete-registrations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Maximum roster limit reached"));
        }
    }

    @Nested
    @DisplayName("GET /v1/athlete-registrations")
    class Find {

        @Test
        @DisplayName("should return 200 with filtered page")
        void shouldReturn200WithPage() throws Exception {
            var registration = aRegistration(RegistrationStatus.APPROVED, "10");
            when(service.findAll(eq(10L), eq(RegistrationStatus.APPROVED), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(registration), PageRequest.of(0, 10), 1));
            when(mapper.toSummary(registration)).thenReturn(aSummary(RegistrationStatus.APPROVED));

            mockMvc.perform(get("/v1/athlete-registrations")
                            .param("teamRegistrationId", "10")
                            .param("status", "APPROVED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].athleteName").value("Francisco da Silva"));
        }

        @Test
        @DisplayName("should return 200 by id")
        void shouldReturn200ById() throws Exception {
            var registration = aRegistration(RegistrationStatus.APPROVED, "10");
            when(service.findByIdOrThrowNotFound(1L)).thenReturn(registration);
            when(mapper.toResponse(registration)).thenReturn(aResponse(RegistrationStatus.APPROVED, "10", null));

            mockMvc.perform(get("/v1/athlete-registrations/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.jerseyNumber").value("10"));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(service.findByIdOrThrowNotFound(99L))
                    .thenThrow(new AthleteRegistrationNotFoundException("Athlete registration not found"));

            mockMvc.perform(get("/v1/athlete-registrations/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Athlete registration not found"));
        }

        @Test
        @DisplayName("should return 400 for a non-positive id")
        void shouldReturn400ForNegativeId() throws Exception {
            mockMvc.perform(get("/v1/athlete-registrations/0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field", containsString("id")));

            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("PUT /v1/athlete-registrations/{id}")
    class Update {

        @Test
        @DisplayName("should update jersey number and return 200")
        void shouldUpdateJerseyNumber() throws Exception {
            var registration = aRegistration(RegistrationStatus.APPROVED, "99");
            when(service.update(1L, "99")).thenReturn(registration);
            when(mapper.toResponse(registration)).thenReturn(aResponse(RegistrationStatus.APPROVED, "99", null));

            mockMvc.perform(put("/v1/athlete-registrations/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"jerseyNumber\":\"99\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jerseyNumber").value("99"));

            verify(service).update(1L, "99");
        }

        @Test
        @DisplayName("should return 400 when jerseyNumber exceeds 3 digits")
        void shouldReturn400WhenJerseyTooLong() throws Exception {
            mockMvc.perform(put("/v1/athlete-registrations/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"jerseyNumber\":\"1000\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("jerseyNumber"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 409 when jerseyNumber is already used on the team")
        void shouldReturn409WhenJerseyNumberAlreadyUsed() throws Exception {
            when(service.update(1L, "7"))
                    .thenThrow(new JerseyNumberAlreadyUsedException("Jersey number already in use"));

            mockMvc.perform(put("/v1/athlete-registrations/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"jerseyNumber\":\"7\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Jersey number already in use"));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/athlete-registrations/{id}/status")
    class ChangeStatus {

        @Test
        @DisplayName("should change status and return 200")
        void shouldChangeStatus() throws Exception {
            var registration = aRegistration(RegistrationStatus.APPROVED, "10");
            when(service.changeStatus(1L, RegistrationStatus.APPROVED)).thenReturn(registration);
            when(mapper.toResponse(registration)).thenReturn(aResponse(RegistrationStatus.APPROVED, "10", null));

            mockMvc.perform(patch("/v1/athlete-registrations/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"APPROVED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));

            verify(service).changeStatus(1L, RegistrationStatus.APPROVED);
        }

        @Test
        @DisplayName("should return 400 when status is missing")
        void shouldReturn400WhenStatusMissing() throws Exception {
            mockMvc.perform(patch("/v1/athlete-registrations/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("status"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 409 when removing the last active athlete of an approved team")
        void shouldReturn409OnLastActiveAthlete() throws Exception {
            when(service.changeStatus(1L, RegistrationStatus.CANCELED))
                    .thenThrow(new LastActiveAthleteException("Cannot remove the last active athlete of an approved team"));

            mockMvc.perform(patch("/v1/athlete-registrations/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"CANCELED\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message")
                            .value("Cannot remove the last active athlete of an approved team"));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/athlete-registrations/{id}/notes")
    class UpdateNotes {

        @Test
        @DisplayName("should save the provided notes")
        void shouldSaveNotes() throws Exception {
            var registration = aRegistration(RegistrationStatus.APPROVED, "10");
            when(service.updateNotes(1L, "Goalkeeper")).thenReturn(registration);
            when(mapper.toResponse(registration))
                    .thenReturn(aResponse(RegistrationStatus.APPROVED, "10", "Goalkeeper"));

            mockMvc.perform(patch("/v1/athlete-registrations/1/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"notes\":\"Goalkeeper\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notes").value("Goalkeeper"));

            verify(service).updateNotes(1L, "Goalkeeper");
        }

        @Test
        @DisplayName("should clear notes when omitted")
        void shouldClearNotes() throws Exception {
            var registration = aRegistration(RegistrationStatus.APPROVED, "10");
            when(service.updateNotes(1L, null)).thenReturn(registration);
            when(mapper.toResponse(registration)).thenReturn(aResponse(RegistrationStatus.APPROVED, "10", null));

            mockMvc.perform(patch("/v1/athlete-registrations/1/notes")
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

            mockMvc.perform(patch("/v1/athlete-registrations/1/notes")
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
            mockMvc.perform(get("/v9/athlete-registrations/1"))
                    .andExpect(status().isBadRequest());
        }
    }
}
