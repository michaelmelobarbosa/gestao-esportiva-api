package br.gov.quixada.sports.category;

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

import br.gov.quixada.sports.category.dto.CategoryCreateRequest;
import br.gov.quixada.sports.category.dto.CategoryResponse;
import br.gov.quixada.sports.category.dto.CategorySummaryResponse;
import br.gov.quixada.sports.category.dto.CategoryUpdateRequest;
import br.gov.quixada.sports.category.exception.CategoryNotFoundException;
import br.gov.quixada.sports.category.exception.DuplicateCategoryException;
import br.gov.quixada.sports.category.exception.InactiveCategoryException;
import br.gov.quixada.sports.category.exception.InvalidAgeException;
import br.gov.quixada.sports.category.exception.InvalidRosterLimitException;
import br.gov.quixada.sports.common.error.ActiveReferenceException;
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
@DisplayName("CategoryController")
class CategoryControllerTest {

    private static final String VALID_CREATE_JSON = """
            {
              "name": "U-17",
              "minAge": 15,
              "maxAge": 17,
              "competitionId": 100
            }
            """;

    private static final String VALID_UPDATE_JSON = """
            {
              "name": "U-18",
              "minAge": 16,
              "maxAge": 18
            }
            """;

    @Mock
    private CategoryService service;

    @Mock
    private CategoryMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new CategoryController(service, mapper);
        mockMvc = ControllerTestSupport.mockMvcValidated(controller);
    }

    private Category aCategory(CategoryStatus status) {
        return Category.builder()
                .id(1L)
                .name("U-17")
                .minAge(15)
                .maxAge(17)
                .status(status)
                .build();
    }

    private CategoryResponse aResponse(CategoryStatus status) {
        return new CategoryResponse(1L, "U-17", 15, 17, null, null, 100L, "Municipal Cup", status);
    }

    private CategorySummaryResponse aSummary(CategoryStatus status) {
        return new CategorySummaryResponse(1L, "U-17", 15, 17, null, null, 100L, status);
    }

    @Nested
    @DisplayName("POST /v1/categories")
    class Save {

        @Test
        @DisplayName("should return 201 with Location header for a valid payload")
        void shouldReturn201WithLocation() throws Exception {
            var category = aCategory(CategoryStatus.ACTIVE);
            when(mapper.toEntity(any(CategoryCreateRequest.class))).thenReturn(category);
            when(service.save(same(category), eq(100L))).thenReturn(category);
            when(mapper.toResponse(category)).thenReturn(aResponse(CategoryStatus.ACTIVE));

            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/v1/categories/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(service).save(category, 100L);
        }

        @Test
        @DisplayName("should return 400 when minAge is negative")
        void shouldReturn400WhenMinAgeIsNegative() throws Exception {
            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON.replace("15", "-1")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("minAge"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 400 when minAthletes is non-positive")
        void shouldReturn400WhenMinAthletesIsNonPositive() throws Exception {
            String json = """
                    {
                      "name": "U-17",
                      "minAge": 15,
                      "maxAge": 17,
                      "competitionId": 100,
                      "minAthletes": 0
                    }
                    """;

            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("minAthletes"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 400 when maxAthletes is non-positive")
        void shouldReturn400WhenMaxAthletesIsNonPositive() throws Exception {
            String json = """
                    {
                      "name": "U-17",
                      "minAge": 15,
                      "maxAge": 17,
                      "competitionId": 100,
                      "maxAthletes": 0
                    }
                    """;

            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("maxAthletes"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 400 when minAge exceeds maxAge")
        void shouldReturn400WhenMinGreaterThanMax() throws Exception {
            var category = aCategory(CategoryStatus.ACTIVE);
            when(mapper.toEntity(any(CategoryCreateRequest.class))).thenReturn(category);
            when(service.save(same(category), eq(100L)))
                    .thenThrow(new InvalidAgeException("Min age cannot be greater than max age"));

            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Min age cannot be greater than max age"));
        }

        @Test
        @DisplayName("should return 409 when an identical active age range exists")
        void shouldReturn409WhenDuplicateAgeRange() throws Exception {
            var category = aCategory(CategoryStatus.ACTIVE);
            when(mapper.toEntity(any(CategoryCreateRequest.class))).thenReturn(category);
            when(service.save(same(category), eq(100L)))
                    .thenThrow(new DuplicateCategoryException("Duplicate age range in competition"));

            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Duplicate age range in competition"));
        }

        @Test
        @DisplayName("should return 400 when minAthletes exceeds maxAthletes")
        void shouldReturn400WhenRosterLimitsInvalid() throws Exception {
            var category = aCategory(CategoryStatus.ACTIVE);
            when(mapper.toEntity(any(CategoryCreateRequest.class))).thenReturn(category);
            when(service.save(same(category), eq(100L)))
                    .thenThrow(new InvalidRosterLimitException("minAthletes cannot exceed maxAthletes"));

            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("minAthletes cannot exceed maxAthletes"));
        }
    }

    @Nested
    @DisplayName("GET /v1/categories")
    class Find {

        @Test
        @DisplayName("should return 200 with filtered page")
        void shouldReturn200WithPage() throws Exception {
            var category = aCategory(CategoryStatus.ACTIVE);
            when(service.findAll(eq("U-17"), eq(CategoryStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(category), PageRequest.of(0, 10), 1));
            when(mapper.toSummary(category)).thenReturn(aSummary(CategoryStatus.ACTIVE));

            mockMvc.perform(get("/v1/categories")
                            .param("name", "U-17")
                            .param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("U-17"));
        }

        @Test
        @DisplayName("should return 200 by id")
        void shouldReturn200ById() throws Exception {
            var category = aCategory(CategoryStatus.ACTIVE);
            when(service.findByIdOrThrowNotFound(1L)).thenReturn(category);
            when(mapper.toResponse(category)).thenReturn(aResponse(CategoryStatus.ACTIVE));

            mockMvc.perform(get("/v1/categories/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.competitionName").value("Municipal Cup"));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(service.findByIdOrThrowNotFound(99L))
                    .thenThrow(new CategoryNotFoundException("Category not found"));

            mockMvc.perform(get("/v1/categories/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Category not found"));
        }

        @Test
        @DisplayName("should return 400 for a non-positive id")
        void shouldReturn400ForNegativeId() throws Exception {
            mockMvc.perform(get("/v1/categories/0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field", containsString("id")));

            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("PUT /v1/categories/{id}")
    class Update {

        @Test
        @DisplayName("should return 200 and delegate to service")
        void shouldReturn200() throws Exception {
            var category = aCategory(CategoryStatus.ACTIVE);
            when(mapper.toEntity(any(CategoryUpdateRequest.class))).thenReturn(category);
            when(service.update(eq(1L), same(category))).thenReturn(category);
            when(mapper.toResponse(category)).thenReturn(aResponse(CategoryStatus.ACTIVE));

            mockMvc.perform(put("/v1/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_UPDATE_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(service).update(1L, category);
        }

        @Test
        @DisplayName("should return 409 when category is inactive")
        void shouldReturn409WhenInactive() throws Exception {
            var category = aCategory(CategoryStatus.INACTIVE);
            when(mapper.toEntity(any(CategoryUpdateRequest.class))).thenReturn(category);
            when(service.update(eq(1L), same(category)))
                    .thenThrow(new InactiveCategoryException("Category is inactive"));

            mockMvc.perform(put("/v1/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_UPDATE_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Category is inactive"));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/categories/{id}/status")
    class ChangeStatus {

        @Test
        @DisplayName("should activate and return 200")
        void shouldActivate() throws Exception {
            var category = aCategory(CategoryStatus.ACTIVE);
            when(service.activate(1L)).thenReturn(category);
            when(mapper.toResponse(category)).thenReturn(aResponse(CategoryStatus.ACTIVE));

            mockMvc.perform(patch("/v1/categories/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"ACTIVE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(service).activate(1L);
        }

        @Test
        @DisplayName("should deactivate and return 200")
        void shouldDeactivate() throws Exception {
            var category = aCategory(CategoryStatus.INACTIVE);
            when(service.deactivate(1L)).thenReturn(category);
            when(mapper.toResponse(category)).thenReturn(aResponse(CategoryStatus.INACTIVE));

            mockMvc.perform(patch("/v1/categories/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"INACTIVE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("INACTIVE"));

            verify(service).deactivate(1L);
        }

        @Test
        @DisplayName("should return 409 when deactivating with active registrations")
        void shouldReturn409OnActiveReference() throws Exception {
            when(service.deactivate(1L))
                    .thenThrow(new ActiveReferenceException("Category has active team registrations"));

            mockMvc.perform(patch("/v1/categories/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"INACTIVE\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Category has active team registrations"));
        }
    }

    @Nested
    @DisplayName("versioning")
    class Versioning {

        @Test
        @DisplayName("should reject an unsupported version with 400")
        void shouldRejectUnsupportedVersion() throws Exception {
            mockMvc.perform(get("/v9/categories/1"))
                    .andExpect(status().isBadRequest());
        }
    }
}
