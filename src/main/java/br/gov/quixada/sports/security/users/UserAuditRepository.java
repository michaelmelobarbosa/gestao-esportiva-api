package br.gov.quixada.sports.security.users;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuditRepository extends JpaRepository<UserAudit, Long> {
}
