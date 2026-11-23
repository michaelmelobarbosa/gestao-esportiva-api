package br.gov.quixada.sports.security.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import br.gov.quixada.sports.security.users.exception.InvalidAuditQueryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAuditService")
class UserAuditServiceTest {

    @Mock
    private UserAuditRepository repository;

    private Clock clock;
    private UserAuditService service;
    private UserAuditRetentionProperties retentionProperties;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Fortaleza"));
        retentionProperties = UserAuditRetentionProperties.defaults();
        service = new UserAuditService(repository, clock, retentionProperties);
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

    @Test
    @DisplayName("should record system audit")
    void shouldRecordSystemAudit() {
        var target = new User("target", "target@example.com", "hash", UserRole.ADMIN);
        target.setId(2L);

        service.recordSystem(target, UserAuditAction.PASSWORD_RESET, "password reset by superadmin CLI");

        var captor = ArgumentCaptor.forClass(UserAudit.class);
        verify(repository).save(captor.capture());
        UserAudit audit = captor.getValue();

        assertThat(audit.getOccurredAt()).isEqualTo(LocalDateTime.of(2026, 6, 15, 9, 0));
        assertThat(audit.getActorId()).isEqualTo(0L);
        assertThat(audit.getActorEmail()).isEqualTo("system:cli");
        assertThat(audit.getAction()).isEqualTo(UserAuditAction.PASSWORD_RESET);
        assertThat(audit.getTargetId()).isEqualTo(2L);
        assertThat(audit.getTargetEmail()).isEqualTo("target@example.com");
    }

    @Test
    @DisplayName("findAll with filters should invoke repository with specification")
    void shouldFindAllWithFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = service.findAll(
                "actor@example.com",
                "target@example.com",
                UserAuditAction.CREATE,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 2, 1, 0, 0),
                pageable
        );

        assertThat(result).isNotNull();
        verify(repository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("findAll should throw InvalidAuditQueryException when from date is after to date")
    void shouldThrowWhenFromIsAfterToInFindAll() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 1, 0, 0);

        assertThatThrownBy(() -> service.findAll(null, null, null, from, to, PageRequest.of(0, 10)))
                .isInstanceOf(InvalidAuditQueryException.class)
                .hasMessage("Start date cannot be after end date");
    }

    @Test
    @DisplayName("findForExport should throw InvalidAuditQueryException when from date is after to date")
    void shouldThrowWhenFromIsAfterToInFindForExport() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 1, 0, 0);

        assertThatThrownBy(() -> service.findForExport(null, null, null, from, to))
                .isInstanceOf(InvalidAuditQueryException.class)
                .hasMessage("Start date cannot be after end date");
    }

    @Test
    @DisplayName("findForExport should query repository with exportMaxRows cap and descending sort")
    void shouldFindForExportWithCap() {
        PageRequest expectedPageable = PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "occurredAt"));
        when(repository.findAll(any(Specification.class), eq(expectedPageable)))
                .thenReturn(new PageImpl<>(List.of()));

        List<UserAudit> result = service.findForExport("actor@test.com", null, null, null, null);

        assertThat(result).isNotNull();
        verify(repository).findAll(any(Specification.class), eq(expectedPageable));
    }

    @Test
    @DisplayName("exportCsv should escape RFC-4180 special characters and neutralize formula injections")
    void shouldExportCsvCorrectly() {
        var audit1 = UserAudit.builder()
                .id(1L)
                .occurredAt(LocalDateTime.of(2026, 6, 15, 9, 30))
                .actorId(10L)
                .actorEmail("admin@example.com")
                .action(UserAuditAction.UPDATE)
                .targetId(20L)
                .targetEmail("user@example.com")
                .changes("value, with comma and \"quotes\"\nand newline")
                .build();

        var audit2 = UserAudit.builder()
                .id(2L)
                .occurredAt(LocalDateTime.of(2026, 6, 15, 10, 0))
                .actorId(10L)
                .actorEmail("admin@example.com")
                .action(UserAuditAction.CREATE)
                .targetId(21L)
                .targetEmail("user2@example.com")
                .changes(null)
                .build();

        var audit3 = UserAudit.builder()
                .id(3L)
                .occurredAt(LocalDateTime.of(2026, 6, 15, 10, 30))
                .actorId(10L)
                .actorEmail("admin@example.com")
                .action(UserAuditAction.UPDATE)
                .targetId(22L)
                .targetEmail("user3@example.com")
                .changes("=cmd|' /C calc'!A0")
                .build();

        var audit4 = UserAudit.builder()
                .id(4L)
                .occurredAt(LocalDateTime.of(2026, 6, 15, 11, 0))
                .actorId(10L)
                .actorEmail("admin@example.com")
                .action(UserAuditAction.UPDATE)
                .targetId(23L)
                .targetEmail("user4@example.com")
                .changes("+1")
                .build();

        var audit5 = UserAudit.builder()
                .id(5L)
                .occurredAt(LocalDateTime.of(2026, 6, 15, 11, 30))
                .actorId(10L)
                .actorEmail("admin@example.com")
                .action(UserAuditAction.UPDATE)
                .targetId(24L)
                .targetEmail("user5@example.com")
                .changes("-1")
                .build();

        var audit6 = UserAudit.builder()
                .id(6L)
                .occurredAt(LocalDateTime.of(2026, 6, 15, 12, 0))
                .actorId(10L)
                .actorEmail("admin@example.com")
                .action(UserAuditAction.UPDATE)
                .targetId(25L)
                .targetEmail("user6@example.com")
                .changes("@SUM(A1:A5)")
                .build();

        String csv = service.exportCsv(List.of(audit1, audit2, audit3, audit4, audit5, audit6));

        assertThat(csv).startsWith("id,occurredAt,actorId,actorEmail,action,targetId,targetEmail,changes\n");

        // row 1: escaped comma, quotes and newline
        assertThat(csv).contains("\"value, with comma and \"\"quotes\"\"\nand newline\"");

        // row 2: null changes rendered as empty string
        assertThat(csv).contains("\n2,2026-06-15T10:00,10,admin@example.com,CREATE,21,user2@example.com,\n");

        // row 3: =cmd neutralized with '
        assertThat(csv).contains("\n3,2026-06-15T10:30,10,admin@example.com,UPDATE,22,user3@example.com,'=cmd|' /C calc'!A0\n");

        // row 4: +1 neutralized with '
        assertThat(csv).contains("\n4,2026-06-15T11:00,10,admin@example.com,UPDATE,23,user4@example.com,'+1\n");

        // row 5: -1 neutralized with '
        assertThat(csv).contains("\n5,2026-06-15T11:30,10,admin@example.com,UPDATE,24,user5@example.com,'-1\n");

        // row 6: @SUM neutralized with '
        assertThat(csv).contains("\n6,2026-06-15T12:00,10,admin@example.com,UPDATE,25,user6@example.com,'@SUM(A1:A5)\n");
    }

    @Test
    @DisplayName("purgeExpired should delete records before retention cutoff date")
    void shouldPurgeExpired() {
        // clock: 2026-06-15T09:00:00 local time
        // cutoff: minus 365 days -> 2025-06-15T09:00:00
        LocalDateTime expectedCutoff = LocalDateTime.of(2025, 6, 15, 9, 0);
        when(repository.deleteByOccurredAtBefore(expectedCutoff)).thenReturn(5L);

        long deleted = service.purgeExpired();

        assertThat(deleted).isEqualTo(5L);
        verify(repository).deleteByOccurredAtBefore(expectedCutoff);
    }

    @Test
    @DisplayName("purgeExpired should return 0 and skip deletion when retentionDays is 0")
    void shouldSkipPurgeWhenRetentionDaysZero() {
        var disabledRetention = new UserAuditRetentionProperties(0, "0 0 3 * * *", 10_000);
        var disabledService = new UserAuditService(repository, clock, disabledRetention);

        long deleted = disabledService.purgeExpired();

        assertThat(deleted).isEqualTo(0L);
        verify(repository, never()).deleteByOccurredAtBefore(any());
    }
}
