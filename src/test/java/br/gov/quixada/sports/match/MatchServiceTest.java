package br.gov.quixada.sports.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.gov.quixada.sports.category.Category;
import br.gov.quixada.sports.category.CategoryRepository;
import br.gov.quixada.sports.category.exception.CategoryNotFoundException;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionRepository;
import br.gov.quixada.sports.competition.CompetitionStatus;
import br.gov.quixada.sports.competition.exception.CompetitionNotEditableException;
import br.gov.quixada.sports.competition.exception.CompetitionNotFoundException;
import br.gov.quixada.sports.team.Team;
import br.gov.quixada.sports.team.TeamStatus;
import br.gov.quixada.sports.registration.TeamRegistration;
import br.gov.quixada.sports.registration.TeamRegistrationRepository;
import br.gov.quixada.sports.registration.RegistrationStatus;
import br.gov.quixada.sports.match.dto.MatchCreateRequest;
import br.gov.quixada.sports.match.dto.MatchResultRequest;
import br.gov.quixada.sports.match.dto.MatchScheduleGenerateRequest;
import br.gov.quixada.sports.match.exception.InvalidMatchException;
import br.gov.quixada.sports.match.exception.MatchNotFoundException;
import br.gov.quixada.sports.match.exception.MatchScheduleAlreadyExistsException;
import br.gov.quixada.sports.match.exception.MatchStatusTransitionException;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService")
class MatchServiceTest {

    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 7, 1, 15, 0);

    @Mock
    private MatchRepository repository;

    @Mock
    private TeamRegistrationRepository teamRegistrationRepository;

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private RoundRobinScheduler roundRobinScheduler;

    private MatchService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Fortaleza"));
        roundRobinScheduler = new RoundRobinScheduler();
        service = new MatchService(
                repository,
                teamRegistrationRepository,
                competitionRepository,
                categoryRepository,
                roundRobinScheduler,
                clock);
    }

    private Competition activeCompetition(Long id) {
        return Competition.builder()
                .id(id)
                .name("Test Cup")
                .registrationStartDate(LocalDate.of(2026, 1, 1))
                .registrationEndDate(LocalDate.of(2026, 2, 1))
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(CompetitionStatus.IN_PROGRESS)
                .build();
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
        when(repository.existsMatchupInRound(anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(false);
        when(repository.existsScheduleConflict(any(), any(), anyLong(), any())).thenReturn(false);
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
        var match = Match.builder().id(1L).scheduledAt(SCHEDULED_AT).status(MatchStatus.SCHEDULED).build();
        when(repository.findById(1L)).thenReturn(Optional.of(match));

        var result = service.changeStatus(1L, MatchStatus.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("should block starting a match when scheduledAt is null")
    void shouldBlockStartMatchWithoutScheduledAt() {
        var match = Match.builder().id(1L).scheduledAt(null).status(MatchStatus.SCHEDULED).build();
        when(repository.findById(1L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.changeStatus(1L, MatchStatus.IN_PROGRESS))
                .isInstanceOf(InvalidMatchException.class)
                .hasMessage("Match must have a scheduled date and time before starting");
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
        when(repository.existsMatchupInRound(anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(false);
        when(repository.existsTeamInRound(any(), any(), anyLong(), any())).thenReturn(true);

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
                .isInstanceOf(MatchNotFoundException.class);
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

    @Nested
    @DisplayName("generateSchedule")
    class GenerateSchedule {

        @Test
        @DisplayName("should generate single round-robin without dates when firstRoundAt is null")
        void shouldGenerateSingleRoundRobinWithoutDates() {
            var competition = activeCompetition(1L);
            var category = Category.builder().id(10L).name("Sub-20").competition(competition).build();
            var t1 = registration(1L, RegistrationStatus.APPROVED, 10L, "Team A");
            var t2 = registration(2L, RegistrationStatus.APPROVED, 10L, "Team B");
            var t3 = registration(3L, RegistrationStatus.APPROVED, 10L, "Team C");

            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
            when(repository.existsActiveByCompetitionAndCategory(1L, 10L, MatchStatus.CANCELED)).thenReturn(false);
            when(teamRegistrationRepository.findByCompetitionIdAndCategoryIdAndStatus(
                    1L, 10L, RegistrationStatus.APPROVED))
                    .thenReturn(List.of(t1, t2, t3));
            when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            var matches = service.generateSchedule(
                    1L, 10L, new MatchScheduleGenerateRequest(false, null, null, "   "));

            assertThat(matches).hasSize(3);
            assertThat(matches).allSatisfy(match -> {
                assertThat(match.getStatus()).isEqualTo(MatchStatus.SCHEDULED);
                assertThat(match.getScheduledAt()).isNull();
                assertThat(match.getVenue()).isNull();
            });
            verify(repository).saveAll(anyList());
        }

        @Test
        @DisplayName("should generate double round-robin with computed round dates and trimmed venue")
        void shouldGenerateDoubleRoundRobinWithDates() {
            var competition = activeCompetition(1L);
            var category = Category.builder().id(10L).name("Sub-20").competition(competition).build();
            var t1 = registration(1L, RegistrationStatus.APPROVED, 10L, "Team A");
            var t2 = registration(2L, RegistrationStatus.APPROVED, 10L, "Team B");

            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
            when(repository.existsActiveByCompetitionAndCategory(1L, 10L, MatchStatus.CANCELED)).thenReturn(false);
            when(teamRegistrationRepository.findByCompetitionIdAndCategoryIdAndStatus(
                    1L, 10L, RegistrationStatus.APPROVED))
                    .thenReturn(List.of(t1, t2));
            when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            var firstRoundAt = LocalDateTime.of(2026, 7, 1, 19, 30);
            var matches = service.generateSchedule(
                    1L, 10L, new MatchScheduleGenerateRequest(true, firstRoundAt, 10, "  Municipal Arena  "));

            assertThat(matches).hasSize(2);
            assertThat(matches.get(0).getRound()).isEqualTo(1);
            assertThat(matches.get(0).getScheduledAt()).isEqualTo(firstRoundAt);
            assertThat(matches.get(0).getVenue()).isEqualTo("Municipal Arena");
            assertThat(matches.get(0).getHomeRegistration()).isSameAs(t1);
            assertThat(matches.get(0).getAwayRegistration()).isSameAs(t2);

            assertThat(matches.get(1).getRound()).isEqualTo(2);
            assertThat(matches.get(1).getScheduledAt()).isEqualTo(firstRoundAt.plusDays(10));
            assertThat(matches.get(1).getVenue()).isEqualTo("Municipal Arena");
            assertThat(matches.get(1).getHomeRegistration()).isSameAs(t2);
            assertThat(matches.get(1).getAwayRegistration()).isSameAs(t1);
        }

        @Test
        @DisplayName("should reject generated schedule when dates exceed competition period")
        void shouldRejectOutOfPeriodDates() {
            var competition = activeCompetition(1L);
            var category = Category.builder().id(10L).name("Sub-20").competition(competition).build();
            var t1 = registration(1L, RegistrationStatus.APPROVED, 10L, "Team A");
            var t2 = registration(2L, RegistrationStatus.APPROVED, 10L, "Team B");

            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
            when(repository.existsActiveByCompetitionAndCategory(1L, 10L, MatchStatus.CANCELED)).thenReturn(false);
            when(teamRegistrationRepository.findByCompetitionIdAndCategoryIdAndStatus(
                    1L, 10L, RegistrationStatus.APPROVED))
                    .thenReturn(List.of(t1, t2));

            var lateStart = LocalDateTime.of(2026, 12, 28, 18, 0);
            assertThatThrownBy(() -> service.generateSchedule(
                    1L, 10L, new MatchScheduleGenerateRequest(true, lateStart, null, null)))
                    .isInstanceOf(InvalidMatchException.class)
                    .hasMessage("Generated schedule exceeds the competition period");

            verify(repository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("should reject when fewer than 2 approved teams exist")
        void shouldRejectFewerThanTwoApprovedTeams() {
            var competition = activeCompetition(1L);
            var category = Category.builder().id(10L).name("Sub-20").competition(competition).build();
            var t1 = registration(1L, RegistrationStatus.APPROVED, 10L, "Team A");

            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
            when(repository.existsActiveByCompetitionAndCategory(1L, 10L, MatchStatus.CANCELED)).thenReturn(false);
            when(teamRegistrationRepository.findByCompetitionIdAndCategoryIdAndStatus(
                    1L, 10L, RegistrationStatus.APPROVED))
                    .thenReturn(List.of(t1));

            assertThatThrownBy(() -> service.generateSchedule(
                    1L, 10L, new MatchScheduleGenerateRequest(false, null, null, null)))
                    .isInstanceOf(InvalidMatchException.class)
                    .hasMessage("At least 2 approved teams are required to generate a schedule");

            verify(repository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("should reject when active matches already exist for competition and category")
        void shouldRejectWhenScheduleAlreadyExists() {
            var competition = activeCompetition(1L);
            var category = Category.builder().id(10L).name("Sub-20").competition(competition).build();

            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
            when(repository.existsActiveByCompetitionAndCategory(1L, 10L, MatchStatus.CANCELED)).thenReturn(true);

            assertThatThrownBy(() -> service.generateSchedule(
                    1L, 10L, new MatchScheduleGenerateRequest(false, null, null, null)))
                    .isInstanceOf(MatchScheduleAlreadyExistsException.class)
                    .hasMessage("Active matches already exist for this competition and category");

            verify(repository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("should reject generating schedule for CANCELED or COMPLETED competition")
        void shouldRejectCanceledOrCompletedCompetition() {
            var canceled = Competition.builder()
                    .id(1L)
                    .name("Canceled Cup")
                    .registrationStartDate(LocalDate.of(2026, 1, 1))
                    .registrationEndDate(LocalDate.of(2026, 2, 1))
                    .startDate(LocalDate.of(2026, 3, 1))
                    .endDate(LocalDate.of(2026, 12, 31))
                    .status(CompetitionStatus.CANCELED)
                    .build();
            var completed = Competition.builder()
                    .id(2L)
                    .name("Completed Cup")
                    .registrationStartDate(LocalDate.of(2026, 1, 1))
                    .registrationEndDate(LocalDate.of(2026, 2, 1))
                    .startDate(LocalDate.of(2026, 3, 1))
                    .endDate(LocalDate.of(2026, 5, 1))
                    .status(CompetitionStatus.COMPLETED)
                    .build();
            var cat1 = Category.builder().id(10L).name("Sub-20").competition(canceled).build();
            var cat2 = Category.builder().id(20L).name("Sub-20").competition(completed).build();

            when(competitionRepository.findById(1L)).thenReturn(Optional.of(canceled));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(cat1));
            when(competitionRepository.findById(2L)).thenReturn(Optional.of(completed));
            when(categoryRepository.findById(20L)).thenReturn(Optional.of(cat2));

            assertThatThrownBy(() -> service.generateSchedule(
                    1L, 10L, new MatchScheduleGenerateRequest(false, null, null, null)))
                    .isInstanceOf(CompetitionNotEditableException.class)
                    .hasMessage("A canceled or completed competition does not allow generating matches");

            assertThatThrownBy(() -> service.generateSchedule(
                    2L, 20L, new MatchScheduleGenerateRequest(false, null, null, null)))
                    .isInstanceOf(CompetitionNotEditableException.class)
                    .hasMessage("A canceled or completed competition does not allow generating matches");
        }

        @Test
        @DisplayName("should reject when category does not belong to the competition")
        void shouldRejectMismatchedCategory() {
            var competition = activeCompetition(1L);
            var otherCompetition = activeCompetition(99L);
            var category = Category.builder().id(10L).name("Sub-20").competition(otherCompetition).build();

            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

            assertThatThrownBy(() -> service.generateSchedule(
                    1L, 10L, new MatchScheduleGenerateRequest(false, null, null, null)))
                    .isInstanceOf(InvalidMatchException.class)
                    .hasMessage("Category does not belong to the informed competition");
        }

        @Test
        @DisplayName("should throw not found when competition or category is missing")
        void shouldThrowNotFoundForMissingCompetitionOrCategory() {
            when(competitionRepository.findById(404L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.generateSchedule(
                    404L, 10L, new MatchScheduleGenerateRequest(false, null, null, null)))
                    .isInstanceOf(CompetitionNotFoundException.class);

            var competition = activeCompetition(1L);
            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(categoryRepository.findById(404L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.generateSchedule(
                    1L, 404L, new MatchScheduleGenerateRequest(false, null, null, null)))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }
}
