package br.gov.quixada.sports.security.users;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserAuditService {

    private final UserAuditRepository repository;
    private final Clock clock;

    @Transactional
    public void record(User actor, User target, UserAuditAction action, String changes) {
        var audit = UserAudit.builder()
                .occurredAt(LocalDateTime.now(clock))
                .actorId(actor.getId())
                .actorEmail(actor.getEmail())
                .action(action)
                .targetId(target.getId())
                .targetEmail(target.getEmail())
                .changes(changes)
                .build();
        repository.save(audit);
    }

    @Transactional
    public void recordSystem(User target, UserAuditAction action, String changes) {
        var audit = UserAudit.builder()
                .occurredAt(LocalDateTime.now(clock))
                .actorId(0L)
                .actorEmail("system:cli")
                .action(action)
                .targetId(target.getId())
                .targetEmail(target.getEmail())
                .changes(changes)
                .build();
        repository.save(audit);
    }

    @Transactional(readOnly = true)
    public Page<UserAudit> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }
}
