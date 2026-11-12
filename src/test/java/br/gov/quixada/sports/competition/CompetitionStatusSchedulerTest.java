package br.gov.quixada.sports.competition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompetitionStatusScheduler")
class CompetitionStatusSchedulerTest {

    @Mock
    private CompetitionRepository repository;

    private CompetitionStatusScheduler scheduler;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Fortaleza"));
        scheduler = new CompetitionStatusScheduler(repository, clock);
    }

    private Competition competition(CompetitionStatus status) {
        return Competition.builder()
                .name("Copa")
                .registrationStartDate(LocalDate.of(2026, 1, 1))
                .registrationEndDate(LocalDate.of(2026, 2, 1))
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 4, 1))
                .status(status)
                .build();
    }

    @Test
    @DisplayName("should sync status by dates and preserve CANCELED")
    void shouldSyncStatusByDates() {
        var completed = competition(CompetitionStatus.PLANNING);
        var canceled = competition(CompetitionStatus.CANCELED);
        when(repository.findAll()).thenReturn(List.of(completed, canceled));

        scheduler.sincronizar();

        assertThat(completed.getStatus()).isEqualTo(CompetitionStatus.COMPLETED);
        assertThat(canceled.getStatus()).isEqualTo(CompetitionStatus.CANCELED);
    }
}
