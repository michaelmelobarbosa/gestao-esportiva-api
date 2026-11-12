package br.gov.quixada.sports.match;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import br.gov.quixada.sports.category.Category;
import br.gov.quixada.sports.club.Club;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionStatus;
import br.gov.quixada.sports.team.Team;
import br.gov.quixada.sports.shared.Address;
import br.gov.quixada.sports.registration.TeamRegistration;
import br.gov.quixada.sports.registration.RegistrationStatus;
import br.gov.quixada.sports.modality.Modality;

@DataJpaTest
@DisplayName("Match — persistence")
class MatchRepositoryTest {

    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 4, 1, 15, 0);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MatchRepository repository;

    private record Scenario(Competition competition, Category category,
                            TeamRegistration registration, Match match) {
    }

    private Scenario scenario() {
        var modality = entityManager.persistFlushFind(Modality.builder().name("Soccer").build());

        var competition = entityManager.persistFlushFind(Competition.builder()
                .name("Test Cup")
                .year(2026)
                .modality(modality)
                .registrationStartDate(LocalDate.of(2026, 1, 1))
                .registrationEndDate(LocalDate.of(2026, 2, 1))
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(CompetitionStatus.REGISTRATION_OPEN)
                .build());

        var category = entityManager.persistFlushFind(Category.builder()
                .name("Sub-20")
                .minAge(18)
                .maxAge(20)
                .competition(competition)
                .build());

        var club = entityManager.persistFlushFind(Club.builder()
                .name("Club A")
                .manager("John")
                .phone("88999999999")
                .address(new Address("A Street", "10", "Downtown", "Quixadá", "63900-000"))
                .build());

        var team = entityManager.persistFlushFind(Team.builder()
                .name("Team A")
                .club(club)
                .modality(modality)
                .manager("John")
                .build());

        var teamRegistration = entityManager.persistFlushFind(TeamRegistration.builder()
                .competition(competition)
                .category(category)
                .team(team)
                .status(RegistrationStatus.APPROVED)
                .build());

        var match = entityManager.persistFlushFind(Match.builder()
                .round(1)
                .scheduledAt(SCHEDULED_AT)
                .venue("Gymnasium")
                .homeRegistration(teamRegistration)
                .awayRegistration(teamRegistration)
                .status(MatchStatus.SCHEDULED)
                .build());

        return new Scenario(competition, category, teamRegistration, match);
    }

    @Test
    @DisplayName("should save match and find by competition/category")
    void shouldFindByCompetitionAndCategory() {
        var scenario = scenario();

        var page = repository.findByCompetitionAndCategory(
                scenario.competition().getId(), scenario.category().getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Match::getId)
                .containsExactly(scenario.match().getId());
    }

    @Test
    @DisplayName("should filter by competition alone")
    void shouldFilterByCompetition() {
        var scenario = scenario();

        var page = repository.search(scenario.competition().getId(), null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("should detect team with a match in the same round")
    void shouldDetectTeamInRound() {
        var scenario = scenario();
        var ids = List.of(scenario.registration().getId());

        assertThat(repository.existsTeamNaRound(ids, 1, -1L, MatchStatus.CANCELED)).isTrue();
        assertThat(repository.existsTeamNaRound(ids, 2, -1L, MatchStatus.CANCELED)).isFalse();
    }

    @Test
    @DisplayName("should detect date and time conflict")
    void shouldDetectScheduledAtConflict() {
        var scenario = scenario();
        var ids = List.of(scenario.registration().getId());

        assertThat(repository.existsConflitoScheduledAt(ids, SCHEDULED_AT, -1L, MatchStatus.CANCELED)).isTrue();
        assertThat(repository.existsConflitoScheduledAt(ids, SCHEDULED_AT.plusHours(1), -1L, MatchStatus.CANCELED))
                .isFalse();
    }

    @Test
    @DisplayName("should cancel matches in cascade by competition and registration")
    void shouldCancelMatchesInCascade() {
        var scenario = scenario();
        var now = LocalDateTime.of(2026, 3, 10, 9, 0);

        repository.cancelByRegistration(scenario.registration().getId(),
                List.of(MatchStatus.SCHEDULED, MatchStatus.IN_PROGRESS), MatchStatus.CANCELED, now);
        entityManager.clear();

        var canceled = repository.findById(scenario.match().getId()).orElseThrow();
        assertThat(canceled.getStatus()).isEqualTo(MatchStatus.CANCELED);
        assertThat(canceled.getCanceledAt()).isEqualTo(now);
    }
}
