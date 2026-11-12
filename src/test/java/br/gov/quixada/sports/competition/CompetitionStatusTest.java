package br.gov.quixada.sports.competition;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Competition — status calculation")
class CompetitionStatusTest {

    private static final LocalDate REGISTRATION_START = LocalDate.of(2026, 1, 1);
    private static final LocalDate REGISTRATION_END = LocalDate.of(2026, 2, 1);
    private static final LocalDate EVENT_START = LocalDate.of(2026, 3, 1);
    private static final LocalDate EVENT_END = LocalDate.of(2026, 4, 1);

    private Competition competition(CompetitionStatus status) {
        return Competition.builder()
                .registrationStartDate(REGISTRATION_START)
                .registrationEndDate(REGISTRATION_END)
                .startDate(EVENT_START)
                .endDate(EVENT_END)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("should calculate PLANNING before registrations open")
    void shouldCalculatePlanningBeforeRegistrations() {
        assertThat(competition(CompetitionStatus.PLANNING).calculateStatus(LocalDate.of(2025, 12, 31)))
                .isEqualTo(CompetitionStatus.PLANNING);
    }

    @Test
    @DisplayName("should calculate REGISTRATION_OPEN within the registration window")
    void shouldCalculateRegistrationOpenWithinWindow() {
        assertThat(competition(CompetitionStatus.PLANNING).calculateStatus(REGISTRATION_START))
                .isEqualTo(CompetitionStatus.REGISTRATION_OPEN);
        assertThat(competition(CompetitionStatus.PLANNING).calculateStatus(REGISTRATION_END))
                .isEqualTo(CompetitionStatus.REGISTRATION_OPEN);
    }

    @Test
    @DisplayName("should calculate REGISTRATION_CLOSED between registration and event")
    void shouldCalculateRegistrationClosedBetweenRegistrationAndEvent() {
        assertThat(competition(CompetitionStatus.PLANNING).calculateStatus(LocalDate.of(2026, 2, 2)))
                .isEqualTo(CompetitionStatus.REGISTRATION_CLOSED);
    }

    @Test
    @DisplayName("should calculate IN_PROGRESS during the event")
    void shouldCalculateInProgressDuringEvent() {
        assertThat(competition(CompetitionStatus.PLANNING).calculateStatus(EVENT_START))
                .isEqualTo(CompetitionStatus.IN_PROGRESS);
        assertThat(competition(CompetitionStatus.PLANNING).calculateStatus(EVENT_END))
                .isEqualTo(CompetitionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("should calculate COMPLETED after the event")
    void shouldCalculateCompletedAfterEvent() {
        assertThat(competition(CompetitionStatus.PLANNING).calculateStatus(EVENT_END.plusDays(1)))
                .isEqualTo(CompetitionStatus.COMPLETED);
    }

    @Test
    @DisplayName("CANCELED is terminal")
    void shouldKeepCanceledAsTerminal() {
        assertThat(competition(CompetitionStatus.CANCELED).calculateStatus(EVENT_END.plusDays(1)))
                .isEqualTo(CompetitionStatus.CANCELED);
    }
}
