package br.gov.quixada.sports.security.users;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(UserAuditRetentionProperties.class)
@RequiredArgsConstructor
public class UserAuditRetentionScheduler {

    private final UserAuditService auditService;

    @Scheduled(cron = "${api.audit.purge-cron:0 0 3 * * *}")
    public void purgeExpired() {
        auditService.purgeExpired();
    }
}
