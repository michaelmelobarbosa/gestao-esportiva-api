package br.gov.quixada.sports.modality;

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

import br.gov.quixada.sports.common.error.ActiveReferenceException;
import br.gov.quixada.sports.modality.dto.ModalityCreateRequest;
import br.gov.quixada.sports.modality.dto.ModalityResponse;
import br.gov.quixada.sports.modality.dto.ModalitySummaryResponse;
import br.gov.quixada.sports.modality.dto.ModalityUpdateRequest;
import br.gov.quixada.sports.modality.exception.InactiveModalityException;
import br.gov.quixada.sports.modality.exception.ModalityNotFoundException;
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
@DisplayName("ModalityController")
class ModalityControllerTest {

    @Mock
    private ModalityService service;

    @Mock
    private ModalityMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new ModalityController(service, mapper);
        mockMvc = ControllerTestSupport.mockMvcValidated(controller);
    }

    private Modality aModality(ModalityStatus status) {
        return Modality.builder()
                .id(1L)
                .name("Futsal")
                .status(status)
                .build();
    }

    private ModalityResponse aResponse(ModalityStatus status) {
        return new ModalityResponse(1L, "Futsal", status);
    }

    private ModalitySummaryResponse aSummary(ModalityStatus status) {
        return new ModalitySummaryResponse(1L, "Futsal", status);
    }

    @Nested
    @DisplayName("POST /v1/modalities")
    class Save {

        @Test
        @DisplayName("should return 201 with Location header for a valid payload")
        void shouldReturn201WithLocation() throws Exception {
            var modality = aModality(ModalityStatus.ACTIVE);
            when(mapper.toEntity(any(ModalityCreateRequest.class))).thenReturn(modality);
            when(service.save(modality)).thenReturn(modality);
            when(mapper.toResponse(modality)).thenReturn(aResponse(ModalityStatus.ACTIVE));

            mockMvc.perform(post("/v1/modalities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Futsal\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/v1/modalities/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(service).save(modality);
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldReturn400WhenNameBlank() throws Exception {
            mockMvc.perform(post("/v1/modalities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("name"));

            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("GET /v1/modalities")
    class Find {

        @Test
        @DisplayName("should return 200 with filtered page")
        void shouldReturn200WithPage() throws Exception {
            var modality = aModality(ModalityStatus.ACTIVE);
            when(service.findAll(eq("Futsal"), eq(ModalityStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(modality), PageRequest.of(0, 10), 1));
            when(mapper.toSummary(modality)).thenReturn(aSummary(ModalityStatus.ACTIVE));

            mockMvc.perform(get("/v1/modalities")
                            .param("name", "Futsal")
                            .param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Futsal"));
        }

        @Test
        @DisplayName("should return 200 by id")
        void shouldReturn200ById() throws Exception {
            var modality = aModality(ModalityStatus.ACTIVE);
            when(service.findByIdOrThrowNotFound(1L)).thenReturn(modality);
            when(mapper.toResponse(modality)).thenReturn(aResponse(ModalityStatus.ACTIVE));

            mockMvc.perform(get("/v1/modalities/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Futsal"));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(service.findByIdOrThrowNotFound(99L))
                    .thenThrow(new ModalityNotFoundException("Modality not found"));

            mockMvc.perform(get("/v1/modalities/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Modality not found"));
        }

        @Test
        @DisplayName("should return 400 for a non-positive id")
        void shouldReturn400ForNegativeId() throws Exception {
            mockMvc.perform(get("/v1/modalities/0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field", containsString("id")));

            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("PUT /v1/modalities/{id}")
    class Update {

        @Test
        @DisplayName("should return 200 and delegate to service")
        void shouldReturn200() throws Exception {
            var modality = aModality(ModalityStatus.ACTIVE);
            when(mapper.toEntity(any(ModalityUpdateRequest.class))).thenReturn(modality);
            when(service.update(eq(1L), same(modality))).thenReturn(modality);
            when(mapper.toResponse(modality)).thenReturn(aResponse(ModalityStatus.ACTIVE));

            mockMvc.perform(put("/v1/modalities/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Volleyball\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(service).update(1L, modality);
        }

        @Test
        @DisplayName("should return 409 when modality is inactive")
        void shouldReturn409WhenInactive() throws Exception {
            var modality = aModality(ModalityStatus.INACTIVE);
            when(mapper.toEntity(any(ModalityUpdateRequest.class))).thenReturn(modality);
            when(service.update(eq(1L), same(modality)))
                    .thenThrow(new InactiveModalityException("Modality is inactive"));

            mockMvc.perform(put("/v1/modalities/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Volleyball\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Modality is inactive"));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/modalities/{id}/status")
    class ChangeStatus {

        @Test
        @DisplayName("should activate and return 200")
        void shouldActivate() throws Exception {
            var modality = aModality(ModalityStatus.ACTIVE);
            when(service.activate(1L)).thenReturn(modality);
            when(mapper.toResponse(modality)).thenReturn(aResponse(ModalityStatus.ACTIVE));

            mockMvc.perform(patch("/v1/modalities/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"ACTIVE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(service).activate(1L);
        }

        @Test
        @DisplayName("should return 409 when deactivating with active teams or competitions")
        void shouldReturn409OnActiveReference() throws Exception {
            when(service.deactivate(1L))
                    .thenThrow(new ActiveReferenceException("Modality has active references"));

            mockMvc.perform(patch("/v1/modalities/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"INACTIVE\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Modality has active references"));
        }
    }

    @Nested
    @DisplayName("versioning")
    class Versioning {

        @Test
        @DisplayName("should reject an unsupported version with 400")
        void shouldRejectUnsupportedVersion() throws Exception {
            mockMvc.perform(get("/v9/modalities/1"))
                    .andExpect(status().isBadRequest());
        }
    }
}
