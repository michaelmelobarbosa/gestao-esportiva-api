package br.gov.quixada.sports.security.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("UserAuditRepository — specification and purge queries")
class UserAuditRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserAuditRepository repository;

    private UserAudit audit1;
    private UserAudit audit2;
    private UserAudit audit3;

    @BeforeEach
    void setUp() {
        audit1 = entityManager.persistFlushFind(UserAudit.builder()
                .occurredAt(LocalDateTime.of(2026, 1, 10, 10, 0))
                .actorId(1L)
                .actorEmail("alice@quixada.gov.br")
                .action(UserAuditAction.CREATE)
                .targetId(2L)
                .targetEmail("bob@quixada.gov.br")
                .changes("created user")
                .build());

        audit2 = entityManager.persistFlushFind(UserAudit.builder()
                .occurredAt(LocalDateTime.of(2026, 2, 15, 11, 0))
                .actorId(3L)
                .actorEmail("charlie@quixada.gov.br")
                .action(UserAuditAction.UPDATE)
                .targetId(1L)
                .targetEmail("alice@quixada.gov.br")
                .changes("updated role")
                .build());

        audit3 = entityManager.persistFlushFind(UserAudit.builder()
                .occurredAt(LocalDateTime.of(2026, 3, 20, 12, 0))
                .actorId(1L)
                .actorEmail("alice@quixada.gov.br")
                .action(UserAuditAction.STATUS_CHANGE)
                .targetId(4L)
                .targetEmail("david@quixada.gov.br")
                .changes("status: ACTIVE -> INACTIVE")
                .build());
    }

    @Test
    @DisplayName("should filter by actorEmail case-insensitively")
    void shouldFilterByActorEmail() {
        var spec = UserAuditSpecifications.withFilters("ALICE", null, null, null, null);
        var result = repository.findAll(spec);

        assertThat(result).extracting(UserAudit::getId).containsExactlyInAnyOrder(audit1.getId(), audit3.getId());
    }

    @Test
    @DisplayName("should filter by targetEmail case-insensitively")
    void shouldFilterByTargetEmail() {
        var spec = UserAuditSpecifications.withFilters(null, "Alice@quixada", null, null, null);
        var result = repository.findAll(spec);

        assertThat(result).extracting(UserAudit::getId).containsExactly(audit2.getId());
    }

    @Test
    @DisplayName("should filter by action")
    void shouldFilterByAction() {
        var spec = UserAuditSpecifications.withFilters(null, null, UserAuditAction.UPDATE, null, null);
        var result = repository.findAll(spec);

        assertThat(result).extracting(UserAudit::getId).containsExactly(audit2.getId());
    }

    @Test
    @DisplayName("should filter by date range")
    void shouldFilterByDateRange() {
        var spec = UserAuditSpecifications.withFilters(
                null, null, null,
                LocalDateTime.of(2026, 1, 15, 0, 0),
                LocalDateTime.of(2026, 2, 28, 23, 59)
        );
        var result = repository.findAll(spec);

        assertThat(result).extracting(UserAudit::getId).containsExactly(audit2.getId());
    }

    @Test
    @DisplayName("should delete audits occurred before cutoff date")
    void shouldDeleteOccurredAtBefore() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 2, 1, 0, 0);
        long deleted = repository.deleteByOccurredAtBefore(cutoff);
        entityManager.flush();
        entityManager.clear();

        assertThat(deleted).isEqualTo(1L);
        assertThat(repository.findAll()).extracting(UserAudit::getId)
                .containsExactlyInAnyOrder(audit2.getId(), audit3.getId());
    }
}
