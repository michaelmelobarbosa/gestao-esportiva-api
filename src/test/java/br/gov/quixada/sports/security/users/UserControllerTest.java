package br.gov.quixada.sports.security.users;

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

import br.gov.quixada.sports.security.auth.exception.EmailAlreadyRegisteredException;
import br.gov.quixada.sports.security.users.dto.UserCreateRequest;
import br.gov.quixada.sports.security.users.dto.UserResponse;
import br.gov.quixada.sports.security.users.dto.UserUpdateRequest;
import br.gov.quixada.sports.security.users.exception.OperationNotAllowedException;
import br.gov.quixada.sports.security.users.exception.UserNotFoundException;
import br.gov.quixada.sports.support.ControllerTestSupport;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController")
class UserControllerTest {

    private static final String VALID_CREATE_JSON = """
            {
              "username": "manager1",
              "email": "manager@quixada.gov.br",
              "password": "strongPassword123",
              "role": "ADMIN"
            }
            """;

    private static final String VALID_UPDATE_JSON = """
            {
              "username": "manager2",
              "email": "manager@quixada.gov.br",
              "role": "ADMIN"
            }
            """;

    @Mock
    private UserService service;

    @Mock
    private UserAuditService auditService;

    @Mock
    private UserMapper mapper;

    private User superAdminActor;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        superAdminActor = new User(1L, "superadmin", "admin@quixada.gov.br", "hash", UserRole.SUPERADMIN, UserStatus.ACTIVE);
        var principal = new UserDetailsAdapter(superAdminActor);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        var controller = new UserController(service, auditService, mapper);
        mockMvc = ControllerTestSupport.mockMvcValidated(controller);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User aUser(UserStatus status) {
        return new User(2L, "manager1", "manager@quixada.gov.br", "hash", UserRole.ADMIN, status);
    }

    private UserResponse aResponse(UserStatus status) {
        return new UserResponse(2L, "manager1", "manager@quixada.gov.br", UserRole.ADMIN, status);
    }

    @Nested
    @DisplayName("POST /v1/users")
    class Save {

        @Test
        @DisplayName("should return 201 with Location header for a valid payload")
        void shouldReturn201WithLocation() throws Exception {
            var created = aUser(UserStatus.ACTIVE);
            when(service.create(same(superAdminActor), any(UserCreateRequest.class))).thenReturn(created);
            when(mapper.toResponse(created)).thenReturn(aResponse(UserStatus.ACTIVE));

            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/v1/users/2"))
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.role").value("ADMIN"));

            verify(service).create(same(superAdminActor), any(UserCreateRequest.class));
        }

        @Test
        @DisplayName("should return 400 when password is shorter than 8 characters")
        void shouldReturn400WhenPasswordTooShort() throws Exception {
            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON.replace("strongPassword123", "short")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("password"));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("should return 403 when attempting to create a SUPERADMIN")
        void shouldReturn403WhenCreatingSuperAdmin() throws Exception {
            when(service.create(same(superAdminActor), any(UserCreateRequest.class)))
                    .thenThrow(new OperationNotAllowedException("Cannot create SUPERADMIN via API"));

            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON.replace("\"ADMIN\"", "\"SUPERADMIN\"")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Cannot create SUPERADMIN via API"));
        }

        @Test
        @DisplayName("should return 409 when email is already registered")
        void shouldReturn409WhenEmailAlreadyRegistered() throws Exception {
            when(service.create(same(superAdminActor), any(UserCreateRequest.class)))
                    .thenThrow(new EmailAlreadyRegisteredException("Email already registered"));

            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CREATE_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Email already registered"));
        }
    }

    @Nested
    @DisplayName("GET /v1/users")
    class Find {

        @Test
        @DisplayName("should return 200 with filtered user page")
        void shouldReturn200WithPage() throws Exception {
            var user = aUser(UserStatus.ACTIVE);
            when(service.findAll(eq("manager"), eq(UserStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));
            when(mapper.toResponse(user)).thenReturn(aResponse(UserStatus.ACTIVE));

            mockMvc.perform(get("/v1/users")
                            .param("search", "manager")
                            .param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(2))
                    .andExpect(jsonPath("$.content[0].email").value("manager@quixada.gov.br"));
        }

        @Test
        @DisplayName("should return 200 with audit page")
        void shouldReturn200WithAuditPage() throws Exception {
            var audit = UserAudit.builder()
                    .id(10L)
                    .occurredAt(LocalDateTime.of(2026, 6, 15, 10, 0))
                    .actorId(1L)
                    .actorEmail("admin@quixada.gov.br")
                    .action(UserAuditAction.CREATE)
                    .targetId(2L)
                    .targetEmail("manager@quixada.gov.br")
                    .changes("role=ADMIN")
                    .build();
            when(auditService.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(audit), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/v1/users/audit"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(10))
                    .andExpect(jsonPath("$.content[0].action").value("CREATE"))
                    .andExpect(jsonPath("$.content[0].targetEmail").value("manager@quixada.gov.br"));
        }

        @Test
        @DisplayName("should return 200 by id")
        void shouldReturn200ById() throws Exception {
            var user = aUser(UserStatus.ACTIVE);
            when(service.findByIdOrThrowNotFound(2L)).thenReturn(user);
            when(mapper.toResponse(user)).thenReturn(aResponse(UserStatus.ACTIVE));

            mockMvc.perform(get("/v1/users/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("manager1"));
        }

        @Test
        @DisplayName("should return 404 when user is not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(service.findByIdOrThrowNotFound(99L))
                    .thenThrow(new UserNotFoundException("User not found"));

            mockMvc.perform(get("/v1/users/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));
        }

        @Test
        @DisplayName("should return 400 for a non-positive id")
        void shouldReturn400ForNegativeId() throws Exception {
            mockMvc.perform(get("/v1/users/0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field", containsString("id")));

            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("PUT /v1/users/{id}")
    class Update {

        @Test
        @DisplayName("should return 200 when updating user without password")
        void shouldReturn200() throws Exception {
            var user = aUser(UserStatus.ACTIVE);
            when(service.update(same(superAdminActor), eq(2L), any(UserUpdateRequest.class))).thenReturn(user);
            when(mapper.toResponse(user)).thenReturn(aResponse(UserStatus.ACTIVE));

            mockMvc.perform(put("/v1/users/2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_UPDATE_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2));

            verify(service).update(same(superAdminActor), eq(2L), any(UserUpdateRequest.class));
        }

        @Test
        @DisplayName("should return 403 when editing a protected user")
        void shouldReturn403WhenNotAllowed() throws Exception {
            when(service.update(same(superAdminActor), eq(2L), any(UserUpdateRequest.class)))
                    .thenThrow(new OperationNotAllowedException("Cannot change own role"));

            mockMvc.perform(put("/v1/users/2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_UPDATE_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Cannot change own role"));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/users/{id}/status")
    class ChangeStatus {

        @Test
        @DisplayName("should change user status and return 200")
        void shouldChangeStatus() throws Exception {
            var user = aUser(UserStatus.INACTIVE);
            when(service.changeStatus(same(superAdminActor), eq(2L), eq(UserStatus.INACTIVE))).thenReturn(user);
            when(mapper.toResponse(user)).thenReturn(aResponse(UserStatus.INACTIVE));

            mockMvc.perform(patch("/v1/users/2/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"INACTIVE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("INACTIVE"));

            verify(service).changeStatus(superAdminActor, 2L, UserStatus.INACTIVE);
        }

        @Test
        @DisplayName("should return 403 when deactivating own SUPERADMIN account")
        void shouldReturn403WhenDeactivatingSelf() throws Exception {
            when(service.changeStatus(same(superAdminActor), eq(1L), eq(UserStatus.INACTIVE)))
                    .thenThrow(new OperationNotAllowedException("Cannot deactivate own account"));

            mockMvc.perform(patch("/v1/users/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"INACTIVE\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Cannot deactivate own account"));
        }
    }

    @Nested
    @DisplayName("versioning")
    class Versioning {

        @Test
        @DisplayName("should reject an unsupported version with 400")
        void shouldRejectUnsupportedVersion() throws Exception {
            mockMvc.perform(get("/v9/users/2"))
                    .andExpect(status().isBadRequest());
        }
    }
}
