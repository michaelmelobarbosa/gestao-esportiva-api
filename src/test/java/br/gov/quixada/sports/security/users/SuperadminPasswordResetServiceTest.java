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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.gov.quixada.sports.security.auth.UserRepository;
import br.gov.quixada.sports.security.users.exception.OperationNotAllowedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuperadminPasswordResetService")
class SuperadminPasswordResetServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserAuditService auditService;

    private SuperadminPasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new SuperadminPasswordResetService(repository, passwordEncoder, auditService);
    }

    private User user(UserRole role, UserStatus status) {
        var user = new User("admin", "admin@quixada.gov.br", "hash-antigo", role);
        user.setId(1L);
        user.setStatus(status);
        return user;
    }

    @Test
    @DisplayName("should reset the SUPERADMIN password, reactivate and audit")
    void shouldResetSuperadminPassword() {
        var superadmin = user(UserRole.SUPERADMIN, UserStatus.INACTIVE);
        when(repository.findByEmail("admin@quixada.gov.br")).thenReturn(Optional.of(superadmin));
        when(passwordEncoder.encode("newPassword123")).thenReturn("hash-novo");

        service.reset("admin@quixada.gov.br", "newPassword123".toCharArray());

        assertThat(superadmin.getPassword()).isEqualTo("hash-novo");
        assertThat(superadmin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(repository).saveAndFlush(superadmin);
        verify(auditService).recordSystem(superadmin, UserAuditAction.PASSWORD_RESET,
                "password reset via recovery CLI");
    }

    @Test
    @DisplayName("should fail when the email does not exist")
    void shouldFailWhenEmailDoesNotExist() {
        when(repository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reset("missing@example.com", "newPassword123".toCharArray()))
                .isInstanceOf(OperationNotAllowedException.class)
                .hasMessage("SUPERADMIN not found for the informed email");

        verify(repository, never()).saveAndFlush(any(User.class));
        verify(auditService, never()).recordSystem(any(), any(), anyString());
    }

    @Test
    @DisplayName("should fail when the user is not SUPERADMIN")
    void shouldFailWhenUserIsNotSuperadmin() {
        var admin = user(UserRole.ADMIN, UserStatus.ACTIVE);
        when(repository.findByEmail("admin@quixada.gov.br")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.reset("admin@quixada.gov.br", "newPassword123".toCharArray()))
                .isInstanceOf(OperationNotAllowedException.class)
                .hasMessage("SUPERADMIN not found for the informed email");

        verify(repository, never()).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("should reject a password outside the allowed range")
    void shouldRejectPasswordOutOfRange() {
        assertThatThrownBy(() -> service.reset("admin@quixada.gov.br", "curta".toCharArray()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must be between 8 and 72 characters");

        verify(repository, never()).findByEmail(anyString());
    }
}
