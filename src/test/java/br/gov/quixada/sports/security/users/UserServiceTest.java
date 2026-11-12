package br.gov.quixada.sports.security.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.gov.quixada.sports.security.auth.UserRepository;
import br.gov.quixada.sports.security.users.dto.UserCreateRequest;
import br.gov.quixada.sports.security.users.dto.UserUpdateRequest;
import br.gov.quixada.sports.security.users.exception.OperationNotAllowedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserAuditService auditService;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository, passwordEncoder, auditService);
    }

    private User user(Long id, UserRole role, UserStatus status) {
        var user = new User("user" + id, "user" + id + "@example.com", "hash", role);
        user.setId(id);
        user.setStatus(status);
        return user;
    }

    private UserUpdateRequest updateRequest(String email, String password, UserRole role) {
        return new UserUpdateRequest("user", email, password, role);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should allow SUPERADMIN to create ADMIN")
        void shouldAllowSuperadminToCreateAdmin() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ACTIVE);
            var request = new UserCreateRequest("novo", "novo@example.com", "password123", UserRole.ADMIN);
            when(repository.existsByEmail("novo@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hash");
            when(repository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                saved.setId(10L);
                return saved;
            });

            var saved = service.create(actor, request);

            assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
            verify(auditService).record(actor, saved, UserAuditAction.CREATE, "role: ADMIN");
        }

        @Test
        @DisplayName("should prohibit creating SUPERADMIN through the API")
        void shouldProhibitCreatingSuperadmin() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ACTIVE);
            var request = new UserCreateRequest("novo", "novo@example.com", "password123", UserRole.SUPERADMIN);

            assertThatThrownBy(() -> service.create(actor, request))
                    .isInstanceOf(OperationNotAllowedException.class)
                    .hasMessage("Creating a SUPERADMIN through the API is not allowed");

            verify(repository, never()).saveAndFlush(any(User.class));
        }

        @Test
        @DisplayName("should prohibit ADMIN from managing users")
        void shouldProhibitAdminCreating() {
            var actor = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
            var request = new UserCreateRequest("novo", "novo@example.com", "password123", UserRole.USER);

            assertThatThrownBy(() -> service.create(actor, request))
                    .isInstanceOf(OperationNotAllowedException.class);

            verify(repository, never()).saveAndFlush(any(User.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should allow SUPERADMIN to edit ADMIN")
        void shouldAllowSuperadminToEditAdmin() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ACTIVE);
            var target = user(2L, UserRole.ADMIN, UserStatus.ACTIVE);
            when(repository.findById(2L)).thenReturn(Optional.of(target));
            when(repository.saveAndFlush(target)).thenReturn(target);

            var request = updateRequest(target.getEmail(), null, UserRole.ADMIN);
            var saved = service.update(actor, 2L, request);

            assertThat(saved.getUsername()).isEqualTo("user");
            verify(auditService).record(actor, saved, UserAuditAction.UPDATE, "username: user2 -> user");
        }

        @Test
        @DisplayName("should prohibit changing your own role")
        void shouldProhibitChangingOwnRole() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ACTIVE);
            var request = updateRequest(actor.getEmail(), null, UserRole.ADMIN);
            when(repository.findById(1L)).thenReturn(Optional.of(actor));

            assertThatThrownBy(() -> service.update(actor, 1L, request))
                    .isInstanceOf(OperationNotAllowedException.class)
                    .hasMessage("Changing your own role is not allowed");

            verify(repository, never()).saveAndFlush(any(User.class));
        }

        @Test
        @DisplayName("should allow SUPERADMIN to edit their own profile keeping the role")
        void shouldAllowEditingOwnProfile() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(actor));
            when(repository.saveAndFlush(actor)).thenReturn(actor);

            var request = updateRequest(actor.getEmail(), null, UserRole.SUPERADMIN);
            service.update(actor, 1L, request);

            verify(auditService).record(actor, actor, UserAuditAction.UPDATE, "username: user1 -> user");
        }

        @Test
        @DisplayName("should prohibit editing another SUPERADMIN")
        void shouldProhibitEditingOtherSuperadmin() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ACTIVE);
            var target = user(2L, UserRole.SUPERADMIN, UserStatus.ACTIVE);
            when(repository.findById(2L)).thenReturn(Optional.of(target));

            var request = updateRequest(target.getEmail(), null, UserRole.SUPERADMIN);

            assertThatThrownBy(() -> service.update(actor, 2L, request))
                    .isInstanceOf(OperationNotAllowedException.class)
                    .hasMessage("Changing another SUPERADMIN is not allowed");

            verify(repository, never()).saveAndFlush(any(User.class));
        }

        @Test
        @DisplayName("should prohibit promoting a user to SUPERADMIN")
        void shouldProhibitPromotingToSuperadmin() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ACTIVE);
            var target = user(2L, UserRole.ADMIN, UserStatus.ACTIVE);
            when(repository.findById(2L)).thenReturn(Optional.of(target));

            var request = updateRequest(target.getEmail(), null, UserRole.SUPERADMIN);

            assertThatThrownBy(() -> service.update(actor, 2L, request))
                    .isInstanceOf(OperationNotAllowedException.class)
                    .hasMessage("Promoting a user to SUPERADMIN is not allowed");

            verify(repository, never()).saveAndFlush(any(User.class));
        }

        @Test
        @DisplayName("should prohibit ADMIN from editing users")
        void shouldProhibitAdminEditing() {
            var actor = user(1L, UserRole.ADMIN, UserStatus.ACTIVE);
            var request = updateRequest("outro@example.com", null, UserRole.ADMIN);

            assertThatThrownBy(() -> service.update(actor, 2L, request))
                    .isInstanceOf(OperationNotAllowedException.class)
                    .hasMessage("Only SUPERADMIN can manage users");
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        @DisplayName("should allow SUPERADMIN to deactivate a regular user")
        void shouldAllowDeactivateUser() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ACTIVE);
            var target = user(2L, UserRole.USER, UserStatus.ACTIVE);
            when(repository.findById(2L)).thenReturn(Optional.of(target));

            var saved = service.changeStatus(actor, 2L, UserStatus.INACTIVE);

            assertThat(saved.getStatus()).isEqualTo(UserStatus.INACTIVE);
            verify(auditService).record(actor, target, UserAuditAction.STATUS_CHANGE,
                    "status: ACTIVE -> INACTIVE");
        }

        @Test
        @DisplayName("should prohibit changing your own status")
        void shouldProhibitChangingOwnStatus() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(actor));

            assertThatThrownBy(() -> service.changeStatus(actor, 1L, UserStatus.INACTIVE))
                    .isInstanceOf(OperationNotAllowedException.class)
                    .hasMessage("Changing your own status is not allowed");
        }

        @Test
        @DisplayName("should prohibit deactivating SUPERADMIN")
        void shouldProhibitDeactivatingSuperadmin() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ACTIVE);
            var target = user(2L, UserRole.SUPERADMIN, UserStatus.ACTIVE);
            when(repository.findById(2L)).thenReturn(Optional.of(target));

            assertThatThrownBy(() -> service.changeStatus(actor, 2L, UserStatus.INACTIVE))
                    .isInstanceOf(OperationNotAllowedException.class)
                    .hasMessage("Deactivating a SUPERADMIN is not allowed");
        }
    }

    @Test
    @DisplayName("should not audit a status change when the status is the same")
    void shouldNotAuditSameStatus() {
        var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ACTIVE);
        var target = user(2L, UserRole.USER, UserStatus.ACTIVE);
        when(repository.findById(2L)).thenReturn(Optional.of(target));

        service.changeStatus(actor, 2L, UserStatus.ACTIVE);

        verify(auditService, never()).record(any(), any(), any(), anyString());
    }
}
