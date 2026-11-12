package br.gov.quixada.sports.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.gov.quixada.sports.category.Category;
import br.gov.quixada.sports.category.CategoryRepository;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionRepository;
import br.gov.quixada.sports.team.Team;
import br.gov.quixada.sports.registration.TeamRegistration;
import br.gov.quixada.sports.registration.TeamRegistrationRepository;
import br.gov.quixada.sports.registration.RegistrationStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("StandingsService")
class StandingsServiceTest {

    @Mock
    private MatchRepository repository;

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TeamRegistrationRepository teamRegistrationRepository;

    private StandingsService service;

    @BeforeEach
    void setUp() {
        service = new StandingsService(repository, competitionRepository, categoryRepository,
                teamRegistrationRepository);
        var competition = Competition.builder().id(1L).name("Copa").build();
        var category = Category.builder().id(10L).name("Sub-20").competition(competition).build();
        lenient().when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        lenient().when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        lenient().when(teamRegistrationRepository.findByCompetitionIdAndCategoryIdAndStatus(
                1L, 10L, RegistrationStatus.APPROVED)).thenReturn(List.of());
    }

    private TeamRegistration registration(Long id, String teamName) {
        var competition = Competition.builder().id(1L).name("Copa").build();
        var category = Category.builder().id(10L).name("Sub-20").competition(competition).build();
        var team = Team.builder().id(id).name(teamName).build();
        return TeamRegistration.builder()
                .id(id)
                .competition(competition)
                .category(category)
                .team(team)
                .build();
    }

    private Match match(Long id, TeamRegistration home, TeamRegistration away,
                            int homeGoals, int awayGoals, MatchStatus status) {
        return Match.builder()
                .id(id)
                .homeRegistration(home)
                .awayRegistration(away)
                .homeGoals(homeGoals)
                .awayGoals(awayGoals)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("should calculate points, goal difference and sort by points/goal difference")
    void shouldCalculateStandings() {
        var a = registration(1L, "Team A");
        var b = registration(2L, "Team B");
        var c = registration(3L, "Team C");

        when(repository.findByCompetitionAndCategory(1L, 10L)).thenReturn(List.of(
                match(1L, a, b, 2, 1, MatchStatus.COMPLETED),
                match(2L, c, a, 1, 0, MatchStatus.COMPLETED),
                match(3L, b, c, 0, 0, MatchStatus.CANCELED)
        ));

        var standings = service.calculate(1L, 10L);

        assertThat(standings).hasSize(3);
        assertThat(standings.get(0).teamName()).isEqualTo("Team C");
        assertThat(standings.get(0).points()).isEqualTo(3);
        assertThat(standings.get(0).goalDifference()).isEqualTo(1);
        assertThat(standings.get(1).teamName()).isEqualTo("Team A");
        assertThat(standings.get(1).points()).isEqualTo(3);
        assertThat(standings.get(1).goalDifference()).isZero();
        assertThat(standings.get(2).teamName()).isEqualTo("Team B");
        assertThat(standings.get(2).points()).isZero();
    }

    @Test
    @DisplayName("should count draws and points percentage")
    void shouldCountDrawsAndPointsPercentage() {
        var a = registration(1L, "Team A");
        var b = registration(2L, "Team B");

        when(repository.findByCompetitionAndCategory(1L, 10L)).thenReturn(List.of(
                match(1L, a, b, 1, 1, MatchStatus.COMPLETED)
        ));

        var standings = service.calculate(1L, 10L);

        assertThat(standings).allSatisfy(row -> {
            assertThat(row.points()).isEqualTo(1);
            assertThat(row.draws()).isEqualTo(1);
            assertThat(row.played()).isEqualTo(1);
            assertThat(row.pointsPercentage()).isEqualTo(33.33);
        });
    }

    @Test
    @DisplayName("should ignore matches that are not completed")
    void shouldIgnoreNonCompletedMatches() {
        var a = registration(1L, "Team A");
        var b = registration(2L, "Team B");

        when(repository.findByCompetitionAndCategory(1L, 10L)).thenReturn(List.of(
                match(1L, a, b, 2, 1, MatchStatus.IN_PROGRESS),
                match(2L, a, b, 2, 1, MatchStatus.SCHEDULED)
        ));

        assertThat(service.calculate(1L, 10L)).isEmpty();
    }

    @Test
    @DisplayName("should include approved team even without matches")
    void shouldIncludeTeamWithoutMatches() {
        var approved = registration(1L, "Team A");
        when(teamRegistrationRepository.findByCompetitionIdAndCategoryIdAndStatus(
                1L, 10L, RegistrationStatus.APPROVED)).thenReturn(List.of(approved));
        when(repository.findByCompetitionAndCategory(1L, 10L)).thenReturn(List.of());

        var standings = service.calculate(1L, 10L);

        assertThat(standings).hasSize(1);
        assertThat(standings.get(0).teamName()).isEqualTo("Team A");
        assertThat(standings.get(0).points()).isZero();
        assertThat(standings.get(0).played()).isZero();
    }

    @Test
    @DisplayName("should throw 404 when competition does not exist")
    void shouldThrowNotFoundWhenCompetitionDoesNotExist() {
        when(competitionRepository.findById(99L)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.calculate(99L, 10L))
                .isInstanceOf(br.gov.quixada.sports.competition.exception.CompetitionNotFoundException.class);
    }

    @Test
    @DisplayName("should throw 404 when category does not exist")
    void shouldThrowNotFoundWhenCategoryDoesNotExist() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.calculate(1L, 99L))
                .isInstanceOf(br.gov.quixada.sports.category.exception.CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("should reject category from another competition")
    void shouldRejectCategoryFromOtherCompetition() {
        var otherCompetition = Competition.builder().id(2L).name("Outra").build();
        var otherCategory = Category.builder().id(10L).name("Sub-20").competition(otherCompetition).build();
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(otherCategory));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.calculate(1L, 10L))
                .isInstanceOf(br.gov.quixada.sports.registration.exception.InvalidRegistrationException.class);
    }
}
