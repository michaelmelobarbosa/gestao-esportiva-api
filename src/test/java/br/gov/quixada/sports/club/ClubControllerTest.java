package br.gov.quixada.sports.club;

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

import br.gov.quixada.sports.club.dto.ClubCreateRequest;
import br.gov.quixada.sports.club.dto.ClubResponse;
import br.gov.quixada.sports.club.dto.ClubSummaryResponse;
import br.gov.quixada.sports.club.dto.ClubUpdateRequest;
import br.gov.quixada.sports.club.exception.ClubNotFoundException;
import br.gov.quixada.sports.club.exception.InactiveClubException;
import br.gov.quixada.sports.common.error.ActiveReferenceException;
import br.gov.quixada.sports.shared.Address;
import br.gov.quixada.sports.shared.AddressResponse;
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
@DisplayName("ClubController")
class ClubControllerTest {

    private static final String VALID_JSON = """
            {
              "name": "Quixadá Esporte Clube",
              "manager": "Carlos Lima",
              "phone": "88999999999",
              "address": {
                "street": "Main Avenue",
                "number": "100",
                "neighborhood": "Downtown",
                "city": "Quixadá",
                "postalCode": "63900-000"
              }
            }
            """;

    @Mock
    private ClubService service;

    @Mock
    private ClubMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new ClubController(service, mapper);
        mockMvc = ControllerTestSupport.mockMvcValidated(controller);
    }

    private Club aClub(ClubStatus status) {
        return Club.builder()
                .id(1L)
                .name("Quixadá Esporte Clube")
                .manager("Carlos Lima")
                .phone("88999999999")
                .address(new Address("Main Avenue", "100", "Downtown", "Quixadá", "63900-000"))
                .status(status)
                .build();
    }

    private ClubResponse aResponse(ClubStatus status) {
        return new ClubResponse(1L, "Quixadá Esporte Clube", "Carlos Lima", "88999999999",
                new AddressResponse("Main Avenue", "100", "Downtown", "Quixadá", "63900-000"), status);
    }

    private ClubSummaryResponse aSummary(ClubStatus status) {
        return new ClubSummaryResponse(1L, "Quixadá Esporte Clube", "Carlos Lima", "88999999999", status);
    }

    @Nested
    @DisplayName("POST /v1/clubs")
    class Save {

        @Test
        @DisplayName("should return 201 with Location header for a valid payload")
        void shouldReturn201WithLocation() throws Exception {
            var club = aClub(ClubStatus.ACTIVE);
            when(mapper.toEntity(any(ClubCreateRequest.class))).thenReturn(club);
            when(service.save(club)).thenReturn(club);
            when(mapper.toResponse(club)).thenReturn(aResponse(ClubStatus.ACTIVE));

            mockMvc.perform(post("/v1/clubs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/v1/clubs/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(service).save(club);
        }

        @Test
        @DisplayName("should return 400 when phone format is invalid")
        void shouldReturn400WhenPhoneInvalid() throws Exception {
            mockMvc.perform(post("/v1/clubs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON.replace("88999999999", "123")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("phone"));

            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("GET /v1/clubs")
    class Find {

        @Test
        @DisplayName("should return 200 with filtered page")
        void shouldReturn200WithPage() throws Exception {
            var club = aClub(ClubStatus.ACTIVE);
            when(service.findAll(eq("Quixadá"), eq(ClubStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(club), PageRequest.of(0, 10), 1));
            when(mapper.toSummary(club)).thenReturn(aSummary(ClubStatus.ACTIVE));

            mockMvc.perform(get("/v1/clubs")
                            .param("name", "Quixadá")
                            .param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].manager").value("Carlos Lima"));
        }

        @Test
        @DisplayName("should return 200 by id")
        void shouldReturn200ById() throws Exception {
            var club = aClub(ClubStatus.ACTIVE);
            when(service.findByIdOrThrowNotFound(1L)).thenReturn(club);
            when(mapper.toResponse(club)).thenReturn(aResponse(ClubStatus.ACTIVE));

            mockMvc.perform(get("/v1/clubs/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.address.city").value("Quixadá"));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(service.findByIdOrThrowNotFound(99L))
                    .thenThrow(new ClubNotFoundException("Club not found"));

            mockMvc.perform(get("/v1/clubs/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Club not found"));
        }

        @Test
        @DisplayName("should return 400 for a non-positive id")
        void shouldReturn400ForNegativeId() throws Exception {
            mockMvc.perform(get("/v1/clubs/0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field", containsString("id")));

            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("PUT /v1/clubs/{id}")
    class Update {

        @Test
        @DisplayName("should return 200 and delegate to service")
        void shouldReturn200() throws Exception {
            var club = aClub(ClubStatus.ACTIVE);
            when(mapper.toEntity(any(ClubUpdateRequest.class))).thenReturn(club);
            when(service.update(eq(1L), same(club))).thenReturn(club);
            when(mapper.toResponse(club)).thenReturn(aResponse(ClubStatus.ACTIVE));

            mockMvc.perform(put("/v1/clubs/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(service).update(1L, club);
        }

        @Test
        @DisplayName("should return 409 when club is inactive")
        void shouldReturn409WhenInactive() throws Exception {
            var club = aClub(ClubStatus.INACTIVE);
            when(mapper.toEntity(any(ClubUpdateRequest.class))).thenReturn(club);
            when(service.update(eq(1L), same(club)))
                    .thenThrow(new InactiveClubException("Club is inactive"));

            mockMvc.perform(put("/v1/clubs/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Club is inactive"));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/clubs/{id}/status")
    class ChangeStatus {

        @Test
        @DisplayName("should activate and return 200")
        void shouldActivate() throws Exception {
            var club = aClub(ClubStatus.ACTIVE);
            when(service.activate(1L)).thenReturn(club);
            when(mapper.toResponse(club)).thenReturn(aResponse(ClubStatus.ACTIVE));

            mockMvc.perform(patch("/v1/clubs/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"ACTIVE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(service).activate(1L);
        }

        @Test
        @DisplayName("should return 409 when deactivating a club with active teams")
        void shouldReturn409OnActiveReference() throws Exception {
            when(service.deactivate(1L))
                    .thenThrow(new ActiveReferenceException("Club has active teams"));

            mockMvc.perform(patch("/v1/clubs/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"INACTIVE\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Club has active teams"));
        }
    }

    @Nested
    @DisplayName("versioning")
    class Versioning {

        @Test
        @DisplayName("should reject an unsupported version with 400")
        void shouldRejectUnsupportedVersion() throws Exception {
            mockMvc.perform(get("/v9/clubs/1"))
                    .andExpect(status().isBadRequest());
        }
    }
}
