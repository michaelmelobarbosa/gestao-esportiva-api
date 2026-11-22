package br.gov.quixada.sports.athlete;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import br.gov.quixada.sports.athlete.dto.AthleteCreateRequest;
import br.gov.quixada.sports.athlete.dto.AthleteResponse;
import br.gov.quixada.sports.athlete.dto.AthleteUpdateRequest;
import br.gov.quixada.sports.athlete.exception.AthleteNotDeletableException;
import br.gov.quixada.sports.athlete.exception.CpfAlreadyRegisteredException;
import br.gov.quixada.sports.athlete.exception.InactiveAthleteException;
import br.gov.quixada.sports.shared.Address;
import br.gov.quixada.sports.shared.Gender;
import br.gov.quixada.sports.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contract tests for {@link AthleteController} mounted standalone (with the
 * {@code GlobalExceptionHandler} and the API versioning strategy from
 * {@link ControllerTestSupport}). The service and the mapper are mocked: the goal
 * is the HTTP contract (status codes, headers, error payload), not the rules,
 * which are covered by {@code AthleteServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AthleteController")
class AthleteControllerTest {

    private static final String VALID_JSON = """
            {
              "fullName": "Francisco da Silva",
              "cpf": "52998224725",
              "birthDate": "2000-06-15",
              "address": {
                "street": "A Street",
                "number": "10",
                "neighborhood": "Downtown",
                "city": "Quixadá",
                "postalCode": "63900-000"
              },
              "gender": "MALE",
              "phone": "88999999999"
            }
            """;

    private static final String UPDATE_JSON = """
            {
              "fullName": "Francisco da Silva",
              "birthDate": "2000-06-15",
              "address": {
                "street": "A Street",
                "number": "10",
                "neighborhood": "Downtown",
                "city": "Quixadá",
                "postalCode": "63900-000"
              },
              "gender": "MALE",
              "phone": "88999999999"
            }
            """;

    @Mock
    private AthleteService service;

    @Mock
    private AthleteMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new AthleteController(service, mapper, ControllerTestSupport.FIXED_CLOCK);
        // mockMvcValidated: the controller is @Validated, so parameter constraints
        // (@Positive, @Pattern) only fire through the AOP proxy the application builds.
        mockMvc = ControllerTestSupport.mockMvcValidated(controller);
    }

    private Athlete anAthlete(AthleteStatus status) {
        return Athlete.builder()
                .id(1L)
                .fullName("Francisco da Silva")
                .cpf("52998224725")
                .birthDate(LocalDate.of(2000, 6, 15))
                .address(new Address("A Street", "10", "Downtown", "Quixadá", "63900-000"))
                .phone("88999999999")
                .gender(Gender.MALE)
                .status(status)
                .build();
    }

    private AthleteResponse aResponse(AthleteStatus status) {
        return new AthleteResponse(1L, "Francisco da Silva", "52998224725",
                LocalDate.of(2000, 6, 15), 26, null, "88999999999",
                Gender.MALE, status, null);
    }

    @Nested
    @DisplayName("POST /v1/athletes")
    class Save {

        @Test
        @DisplayName("should return 201 with a Location header for a valid payload")
        void shouldReturn201WithLocation() throws Exception {
            var athlete = anAthlete(AthleteStatus.ACTIVE);
            when(mapper.toEntity(any(AthleteCreateRequest.class))).thenReturn(athlete);
            when(service.save(athlete)).thenReturn(athlete);
            when(mapper.toGetResponse(athlete, ControllerTestSupport.FIXED_CLOCK))
                    .thenReturn(aResponse(AthleteStatus.ACTIVE));

            mockMvc.perform(post("/v1/athletes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/v1/athletes/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.age").value(26));

            verify(service).save(athlete);
        }

        @Test
        @DisplayName("should return 400 with the offending field when the name is blank")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            mockMvc.perform(post("/v1/athletes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON.replace("\"Francisco da Silva\"", "\"\"")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("fullName"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 400 when the CPF check digit is invalid")
        void shouldReturn400WhenCpfIsInvalid() throws Exception {
            mockMvc.perform(post("/v1/athletes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON.replace("52998224725", "11111111111")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("cpf"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 409 when the CPF is already registered")
        void shouldReturn409WhenCpfAlreadyRegistered() throws Exception {
            var athlete = anAthlete(AthleteStatus.ACTIVE);
            when(mapper.toEntity(any(AthleteCreateRequest.class))).thenReturn(athlete);
            when(service.save(athlete)).thenThrow(new CpfAlreadyRegisteredException("CPF already registered"));

            mockMvc.perform(post("/v1/athletes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("CPF already registered"));
        }
    }

    @Nested
    @DisplayName("GET /v1/athletes")
    class Find {

        @Test
        @DisplayName("should return 200 with the athlete")
        void shouldReturn200ById() throws Exception {
            var athlete = anAthlete(AthleteStatus.ACTIVE);
            when(service.findByIdOrThrowNotFound(1L)).thenReturn(athlete);
            when(mapper.toGetResponse(athlete, ControllerTestSupport.FIXED_CLOCK))
                    .thenReturn(aResponse(AthleteStatus.ACTIVE));

            mockMvc.perform(get("/v1/athletes/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cpf").value("52998224725"))
                    .andExpect(jsonPath("$.age").value(26));
        }

        @Test
        @DisplayName("should return 400 for a non-positive id")
        void shouldReturn400ForNegativeId() throws Exception {
            mockMvc.perform(get("/v1/athletes/-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation error"))
                    .andExpect(jsonPath("$.errors[0].field", containsString("id")));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 200 when looking up by a formatted CPF")
        void shouldReturn200ByCpf() throws Exception {
            var athlete = anAthlete(AthleteStatus.ACTIVE);
            when(service.findByCpfOrThrowNotFound("529.982.247-25")).thenReturn(athlete);
            when(mapper.toGetResponse(athlete, ControllerTestSupport.FIXED_CLOCK))
                    .thenReturn(aResponse(AthleteStatus.ACTIVE));

            mockMvc.perform(get("/v1/athletes/cpf/529.982.247-25"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("Francisco da Silva"));
        }

        @Test
        @DisplayName("should return 400 for a malformed CPF path variable")
        void shouldReturn400ForMalformedCpf() throws Exception {
            mockMvc.perform(get("/v1/athletes/cpf/abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field", containsString("cpf")));

            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("PATCH /v1/athletes/{id}/status")
    class ChangeStatus {

        @Test
        @DisplayName("should deactivate and return 200")
        void shouldDeactivate() throws Exception {
            var athlete = anAthlete(AthleteStatus.INACTIVE);
            when(service.deactivate(1L)).thenReturn(athlete);
            when(mapper.toGetResponse(athlete, ControllerTestSupport.FIXED_CLOCK))
                    .thenReturn(aResponse(AthleteStatus.INACTIVE));

            mockMvc.perform(patch("/v1/athletes/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"INACTIVE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("INACTIVE"));

            verify(service).deactivate(1L);
        }

        @Test
        @DisplayName("should activate and return 200")
        void shouldActivate() throws Exception {
            var athlete = anAthlete(AthleteStatus.ACTIVE);
            when(service.activate(1L)).thenReturn(athlete);
            when(mapper.toGetResponse(athlete, ControllerTestSupport.FIXED_CLOCK))
                    .thenReturn(aResponse(AthleteStatus.ACTIVE));

            mockMvc.perform(patch("/v1/athletes/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"ACTIVE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(service).activate(1L);
        }

        @Test
        @DisplayName("should return 400 when the status is missing")
        void shouldReturn400WhenStatusIsMissing() throws Exception {
            mockMvc.perform(patch("/v1/athletes/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("status"));

            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("PUT /v1/athletes/{id}")
    class Update {

        @Test
        @DisplayName("should return 200 and delegate to the service")
        void shouldReturn200() throws Exception {
            var athlete = anAthlete(AthleteStatus.ACTIVE);
            when(mapper.toEntity(any(AthleteUpdateRequest.class))).thenReturn(athlete);
            when(service.update(eq(1L), same(athlete))).thenReturn(athlete);
            when(mapper.toGetResponse(athlete, ControllerTestSupport.FIXED_CLOCK))
                    .thenReturn(aResponse(AthleteStatus.ACTIVE));

            mockMvc.perform(put("/v1/athletes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(service).update(1L, athlete);
        }

        @Test
        @DisplayName("should return 409 when the athlete is inactive")
        void shouldReturn409WhenInactive() throws Exception {
            var athlete = anAthlete(AthleteStatus.INACTIVE);
            when(mapper.toEntity(any(AthleteUpdateRequest.class))).thenReturn(athlete);
            when(service.update(eq(1L), same(athlete)))
                    .thenThrow(new InactiveAthleteException("Athlete is inactive; reactivate it before editing"));

            mockMvc.perform(put("/v1/athletes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Athlete is inactive; reactivate it before editing"));
        }
    }

    @Nested
    @DisplayName("DELETE /v1/athletes/{id}")
    class HardDelete {

        @Test
        @DisplayName("should return 204 when the deletion is allowed")
        void shouldReturn204() throws Exception {
            mockMvc.perform(delete("/v1/athletes/1"))
                    .andExpect(status().isNoContent());

            verify(service).hardDelete(1L);
        }

        @Test
        @DisplayName("should return 409 when the athlete cannot be deleted")
        void shouldReturn409() throws Exception {
            doThrow(new AthleteNotDeletableException("An active athlete cannot be deleted; deactivate it first"))
                    .when(service).hardDelete(1L);

            mockMvc.perform(delete("/v1/athletes/1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message")
                            .value("An active athlete cannot be deleted; deactivate it first"));
        }
    }

    @Nested
    @DisplayName("versioning")
    class Versioning {

        @Test
        @DisplayName("should reject an unsupported version with 400")
        void shouldRejectUnsupportedVersion() throws Exception {
            mockMvc.perform(get("/v9/athletes/1"))
                    .andExpect(status().isBadRequest());
        }
    }
}
