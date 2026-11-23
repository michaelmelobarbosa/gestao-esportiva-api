package br.gov.quixada.sports.security.users;

import br.gov.quixada.sports.security.users.exception.InvalidAuditQueryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserAuditService {

    private final UserAuditRepository repository;
    private final Clock clock;
    private final UserAuditRetentionProperties retentionProperties;

    public UserAuditService(UserAuditRepository repository, Clock clock) {
        this(repository, clock, UserAuditRetentionProperties.defaults());
    }

    @Autowired
    public UserAuditService(UserAuditRepository repository, Clock clock, @Autowired(required = false) UserAuditRetentionProperties retentionProperties) {
        this.repository = repository;
        this.clock = clock;
        this.retentionProperties = retentionProperties != null ? retentionProperties : UserAuditRetentionProperties.defaults();
    }

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
        return findAll(null, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<UserAudit> findAll(String actorEmail, String targetEmail, UserAuditAction action, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidAuditQueryException("Start date cannot be after end date");
        }
        return repository.findAll(UserAuditSpecifications.withFilters(actorEmail, targetEmail, action, from, to), pageable);
    }

    @Transactional(readOnly = true)
    public List<UserAudit> findForExport(String actorEmail, String targetEmail, UserAuditAction action, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidAuditQueryException("Start date cannot be after end date");
        }
        var pageRequest = PageRequest.of(0, retentionProperties.exportMaxRows(), Sort.by(Sort.Direction.DESC, "occurredAt"));
        return repository.findAll(UserAuditSpecifications.withFilters(actorEmail, targetEmail, action, from, to), pageRequest).getContent();
    }

    public String exportCsv(List<UserAudit> audits) {
        var sb = new StringBuilder();
        sb.append("id,occurredAt,actorId,actorEmail,action,targetId,targetEmail,changes\n");
        if (audits != null) {
            for (UserAudit audit : audits) {
                sb.append(formatCsvCell(audit.getId())).append(',')
                        .append(formatCsvCell(audit.getOccurredAt())).append(',')
                        .append(formatCsvCell(audit.getActorId())).append(',')
                        .append(formatCsvCell(audit.getActorEmail())).append(',')
                        .append(formatCsvCell(audit.getAction())).append(',')
                        .append(formatCsvCell(audit.getTargetId())).append(',')
                        .append(formatCsvCell(audit.getTargetEmail())).append(',')
                        .append(formatCsvCell(audit.getChanges()))
                        .append('\n');
            }
        }
        return sb.toString();
    }

    @Transactional
    public long purgeExpired() {
        if (retentionProperties.retentionDays() <= 0) {
            return 0L;
        }
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(retentionProperties.retentionDays());
        return repository.deleteByOccurredAtBefore(cutoff);
    }

    private String formatCsvCell(Object value) {
        if (value == null) {
            return "";
        }
        String str = value.toString();
        if (str.isEmpty()) {
            return "";
        }
        char first = str.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            str = "'" + str;
        }
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            str = "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }
}
