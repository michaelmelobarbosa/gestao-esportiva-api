package br.gov.quixada.sports.security.users.dto;

import br.gov.quixada.sports.security.users.UserAudit;
import br.gov.quixada.sports.security.users.UserAuditAction;

import java.time.LocalDateTime;

public record UserAuditResponse(
        Long id,
        LocalDateTime occurredAt,
        Long actorId,
        String actorEmail,
        UserAuditAction action,
        Long targetId,
        String targetEmail,
        String changes
) {

    public static UserAuditResponse from(UserAudit audit) {
        return new UserAuditResponse(
                audit.getId(),
                audit.getOccurredAt(),
                audit.getActorId(),
                audit.getActorEmail(),
                audit.getAction(),
                audit.getTargetId(),
                audit.getTargetEmail(),
                audit.getChanges()
        );
    }
}
