package br.gov.quixada.sports.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.gov.quixada.sports.category.Category;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionStatus;
import br.gov.quixada.sports.team.Team;
import br.gov.quixada.sports.team.TeamStatus;
import br.gov.quixada.sports.registration.TeamRegistration;
import br.gov.quixada.sports.registration.TeamRegistrationRepository;
import br.gov.quixada.sports.registration.RegistrationStatus;
import br.gov.quixada.sports.match.dto.MatchCreateRequest;
import br.gov.quixada.sports.match.dto.MatchResultRequest;
import br.gov.quixada.sports.match.exception.InvalidMatchException;
import br.gov.quixada.sports.match.exception.MatchStatusTransitionException;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService")
class MatchServiceTest {

    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 7, 1, 15, 0);

    @Mock
    private MatchRepository repository;

    @Mock
    private TeamRegistrationRepository teamRegistrationRepository;

    private MatchService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Fortaleza"));
        service = new MatchService(repository, teamRegistrationRepository, clock);
    }

    private TeamRegistration registration(Long id, RegistrationStatus status, Long categoryId, String teamName) {
        return registration(id, status, categoryId, teamName, CompetitionStatus.REGISTRATION_OPEN);
    }

    private TeamRegistration registration(Long id, RegistrationStatus status, Long categoryId, String teamName,
                                      CompetitionStatus competitionStatus) {
        var competition = Competition.builder()
                .id(1L)
                .name("Test Cup")
                .registrationStartDate(LocalDate.of(2026, 1, 1))
                .registrationEndDate(LocalDate.of(2026, 2, 1))
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(competitionStatus)
                .build();
        var category = Category.builder()
                .id(categoryId)
                .name("Sub-20")
                .competition(competition)
                .build();
        var team = Team.builder()
                .id(id)
                .name(teamName)
                .status(TeamStatus.ACTIVE)
                .build();
        return TeamRegistration.builder()
                .id(id)
                .status(status)
                .competition(competition)
                .category(category)
                .team(team)
                .build();
    }

    private void stubCreateWithoutConflict() {
        when(repository.existsConfrontoNaRound(anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(false);
        when(repository.existsConflitoScheduledAt(any(), any(), anyLong(), any())).thenReturn(false);
        when(repository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("should create match with approved registrations from the same competition and category")
    void shouldCreateValidMatch() {
        var home = registration(1L, RegistrationStatus.APPROVED, 10L, "Team A");
        var away = registration(2L, RegistrationStatus.APPROVED, 10L, "Team B");
        when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(home));
        when(teamRegistrationRepository.findById(2L)).thenReturn(Optional.of(away));
        stubCreateWithoutConflict();

        var match = service.create(new MatchCreateRequest(1L, 2L, 1, SCHEDULED_AT, "Gymnasium", null));

        assertThat(match.getStatus()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(match.getHomeRegistration()).isSameAs(home);
        assertThat(match.getAwayRegistration()).isSameAs(away);
    }

    @Test
    @DisplayName("should reject teams without an approved registration")
    void shouldRejectNonApprovedRegistration() {
        var home = registration(1L, RegistrationStatus.PENDING, 10L, "Team A");
        var away = registration(2L, RegistrationStatus.APPROVED, 10L, "Team B");
        when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(home));
        when(teamRegistrationRepository.findById(2L)).thenReturn(Optional.of(away));

        assertThatThrownBy(() -> service.create(new MatchCreateRequest(1L, 2L, 1, SCHEDULED_AT, null, null)))
                .isInstanceOf(InvalidMatchException.class)
                .hasMessage("Both teams must have an APPROVED registration");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("should reject equal home and away")
    void shouldRejectSameTeams() {
        var home = registration(1L, RegistrationStatus.APPROVED, 10L, "Team A");
        when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(home));

        assertThatThrownBy(() -> service.create(new MatchCreateRequest(1L, 1L, 1, SCHEDULED_AT, null, null)))
                .isInstanceOf(InvalidMatchException.class)
                .hasMessage("Home and away teams must be different");
    }

    @Test
    @DisplayName("should start a scheduled match")
    void shouldStartMatch() {
        var match = Match.builder().id(1L).status(MatchStatus.SCHEDULED).build();
        when(repository.findById(1L)).thenReturn(Optional.of(match));

        var result = service.changeStatus(1L, MatchStatus.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("should block invalid transition")
    void shouldBlockInvalidTransition() {
        var match = Match.builder().id(1L).status(MatchStatus.CANCELED).build();
        when(repository.findById(1L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.changeStatus(1L, MatchStatus.IN_PROGRESS))
                .isInstanceOf(MatchStatusTransitionException.class);
    }

    @Test
    @DisplayName("should record result and complete match in progress")
    void shouldRecordResult() {
        var match = Match.builder().id(1L).status(MatchStatus.IN_PROGRESS).build();
        when(repository.findById(1L)).thenReturn(Optional.of(match));

        var result = service.recordResult(1L, new MatchResultRequest(2, 1));

        assertThat(result.getStatus()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(result.getHomeGoals()).isEqualTo(2);
        assertThat(result.getAwayGoals()).isEqualTo(1);
    }

    @Test
    @DisplayName("should reject result for a still scheduled match")
    void shouldRejectResultForScheduledMatch() {
        var match = Match.builder().id(1L).status(MatchStatus.SCHEDULED).build();
        when(repository.findById(1L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.recordResult(1L, new MatchResultRequest(1, 0)))
                .isInstanceOf(InvalidMatchException.class)
                .hasMessage("Results can only be recorded for an IN_PROGRESS match");
    }

    @Test
    @DisplayName("should block team with another match in the same round")
    void shouldBlockTeamInSameRound() {
        var home = registration(1L, RegistrationStatus.APPROVED, 10L, "Team A");
        var away = registration(2L, RegistrationStatus.APPROVED, 10L, "Team B");
        when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(home));
        when(teamRegistrationRepository.findById(2L)).thenReturn(Optional.of(away));
        when(repository.existsConfrontoNaRound(anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(false);
        when(repository.existsTeamNaRound(any(), any(), anyLong(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.create(new MatchCreateRequest(1L, 2L, 1, SCHEDULED_AT, null, null)))
                .isInstanceOf(InvalidMatchException.class)
                .hasMessage("One of the teams already has a match in this round");
    }

    @Test
    @DisplayName("should allow late registration in a canceled competition creating a CANCELED match")
    void shouldCreateCanceledMatchInCanceledCompetition() {
        var home = registration(1L, RegistrationStatus.APPROVED, 10L, "Team A", CompetitionStatus.CANCELED);
        var away = registration(2L, RegistrationStatus.APPROVED, 10L, "Team B", CompetitionStatus.CANCELED);
        when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(home));
        when(teamRegistrationRepository.findById(2L)).thenReturn(Optional.of(away));
        stubCreateWithoutConflict();

        var match = service.create(new MatchCreateRequest(1L, 2L, 1, SCHEDULED_AT, null, null));

        assertThat(match.getStatus()).isEqualTo(MatchStatus.CANCELED);
    }

    @Test
    @DisplayName("should allow late registration in a completed competition")
    void shouldCreateMatchInCompletedCompetition() {
        var home = registration(1L, RegistrationStatus.APPROVED, 10L, "Team A", CompetitionStatus.COMPLETED);
        var away = registration(2L, RegistrationStatus.APPROVED, 10L, "Team B", CompetitionStatus.COMPLETED);
        when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(home));
        when(teamRegistrationRepository.findById(2L)).thenReturn(Optional.of(away));
        stubCreateWithoutConflict();

        var match = service.create(new MatchCreateRequest(1L, 2L, 1, SCHEDULED_AT, null, null));

        assertThat(match.getStatus()).isEqualTo(MatchStatus.SCHEDULED);
    }

    @Test
    @DisplayName("should pass isolated filters to the search")
    void shouldPassIsolatedFilters() {
        when(repository.search(1L, null, null, org.springframework.data.domain.PageRequest.of(0, 10)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        service.findAll(1L, null, null, org.springframework.data.domain.PageRequest.of(0, 10));

        verify(repository).search(1L, null, null, org.springframework.data.domain.PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("should reject result with null goals")
    void shouldRejectResultWithNullGoals() {
        var match = Match.builder().id(1L).status(MatchStatus.IN_PROGRESS).build();
        when(repository.findById(1L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.recordResult(1L, new MatchResultRequest(null, 1)))
                .isInstanceOf(InvalidMatchException.class);
    }

    @Test
    @DisplayName("should store the notes informed on creation")
    void shouldStoreNotesOnCreate() {
        var home = registration(1L, RegistrationStatus.APPROVED, 10L, "Team A");
        var away = registration(2L, RegistrationStatus.APPROVED, 10L, "Team B");
        when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(home));
        when(teamRegistrationRepository.findById(2L)).thenReturn(Optional.of(away));
        stubCreateWithoutConflict();

        var match = service.create(new MatchCreateRequest(1L, 2L, 1, SCHEDULED_AT, "Gymnasium",
                "Opening match"));

        assertThat(match.getNotes()).isEqualTo("Opening match");
    }

    @Test
    @DisplayName("should update the free-form notes on a CANCELED match")
    void shouldUpdateNotesOnCanceledMatch() {
        var match = Match.builder().id(1L).status(MatchStatus.CANCELED).build();
        when(repository.findById(1L)).thenReturn(Optional.of(match));

        var result = service.updateNotes(1L, "Postponed due to lack of referees");

        assertThat(result.getNotes()).isEqualTo("Postponed due to lack of referees");
        verify(repository, never()).save(any(Match.class));
    }

    @Test
    @DisplayName("should clear the notes when null")
    void shouldClearNotes() {
        var match = Match.builder().id(1L).status(MatchStatus.SCHEDULED)
                .notes("old text").build();
        when(repository.findById(1L)).thenReturn(Optional.of(match));

        var result = service.updateNotes(1L, null);

        assertThat(result.getNotes()).isNull();
    }

    @Test
    @DisplayName("should throw not found when updating notes for a missing match")
    void shouldThrowNotFoundWhenUpdatingNotes() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateNotes(9L, "x"))
                .isInstanceOf(br.gov.quixada.sports.match.exception.MatchNotFoundException.class);
    }

    @Test
    @DisplayName("should record the cancellation date when canceling the match")
    void shouldRecordCancellationDate() {
        var match = Match.builder().id(1L).status(MatchStatus.SCHEDULED).build();
        when(repository.findById(1L)).thenReturn(Optional.of(match));

        var result = service.changeStatus(1L, MatchStatus.CANCELED);

        assertThat(result.getStatus()).isEqualTo(MatchStatus.CANCELED);
        assertThat(result.getCanceledAt()).isNotNull();
    }
}
