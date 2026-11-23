package br.gov.quixada.sports.security.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAuditRetentionScheduler")
class UserAuditRetentionSchedulerTest {

    @Mock
    private UserAuditService auditService;

    @InjectMocks
    private UserAuditRetentionScheduler scheduler;

    @Test
    @DisplayName("should delegate purge to auditService.purgeExpired")
    void shouldDelegatePurge() {
        scheduler.purgeExpired();
        verify(auditService).purgeExpired();
    }
}
