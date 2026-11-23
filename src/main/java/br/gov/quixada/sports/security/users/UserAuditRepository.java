package br.gov.quixada.sports.security.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

public interface UserAuditRepository extends JpaRepository<UserAudit, Long>, JpaSpecificationExecutor<UserAudit> {
    long deleteByOccurredAtBefore(LocalDateTime cutoff);
}
