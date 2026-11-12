package br.gov.quixada.sports.security.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAuditService")
class UserAuditServiceTest {

    @Mock
    private UserAuditRepository repository;

    private UserAuditService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Fortaleza"));
        service = new UserAuditService(repository, clock);
    }

    @Test
    @DisplayName("should record actor, target, action and changes without the password")
    void shouldRecordAudit() {
        var actor = new User("admin", "admin@example.com", "hash", UserRole.SUPERADMIN);
        actor.setId(1L);
        var target = new User("alvo", "alvo@example.com", "hash", UserRole.ADMIN);
        target.setId(2L);

        service.record(actor, target, UserAuditAction.UPDATE, "role: USER -> ADMIN");

        var captor = ArgumentCaptor.forClass(UserAudit.class);
        verify(repository).save(captor.capture());
        UserAudit audit = captor.getValue();

        assertThat(audit.getOccurredAt()).isEqualTo(LocalDateTime.of(2026, 6, 15, 9, 0));
        assertThat(audit.getActorId()).isEqualTo(1L);
        assertThat(audit.getActorEmail()).isEqualTo("admin@example.com");
        assertThat(audit.getAction()).isEqualTo(UserAuditAction.UPDATE);
        assertThat(audit.getTargetId()).isEqualTo(2L);
        assertThat(audit.getTargetEmail()).isEqualTo("alvo@example.com");
        assertThat(audit.getChanges()).isEqualTo("role: USER -> ADMIN");
        assertThat(audit.getChanges()).doesNotContain("hash");
    }
}
