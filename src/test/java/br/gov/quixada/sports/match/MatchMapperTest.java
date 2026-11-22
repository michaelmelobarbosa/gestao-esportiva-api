package br.gov.quixada.sports.match;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import br.gov.quixada.sports.category.Category;
import br.gov.quixada.sports.category.CategoryStatus;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionStatus;
import br.gov.quixada.sports.registration.RegistrationStatus;
import br.gov.quixada.sports.registration.TeamRegistration;
import br.gov.quixada.sports.team.Team;
import br.gov.quixada.sports.team.TeamStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatchMapper")
class MatchMapperTest {

    private MatchMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MatchMapperImpl();
    }

    private Match aMatch() {
        var competition = Competition.builder()
                .id(100L)
                .name("Municipal Cup")
                .status(CompetitionStatus.IN_PROGRESS)
                .build();

        var category = Category.builder()
                .id(200L)
                .name("U-20")
                .competition(competition)
                .status(CategoryStatus.ACTIVE)
                .build();

        var homeTeam = Team.builder()
                .id(101L)
                .name("Lions FC")
                .status(TeamStatus.ACTIVE)
                .build();

        var awayTeam = Team.builder()
                .id(102L)
                .name("Eagles FC")
                .status(TeamStatus.ACTIVE)
                .build();

        var homeReg = TeamRegistration.builder()
                .id(10L)
                .competition(competition)
                .category(category)
                .team(homeTeam)
                .status(RegistrationStatus.APPROVED)
                .build();

        var awayReg = TeamRegistration.builder()
                .id(20L)
                .competition(competition)
                .category(category)
                .team(awayTeam)
                .status(RegistrationStatus.APPROVED)
                .build();

        return Match.builder()
                .id(1L)
                .round(3)
                .scheduledAt(LocalDateTime.of(2026, 6, 20, 19, 30))
                .venue("Municipal Arena")
                .status(MatchStatus.COMPLETED)
                .homeGoals(2)
                .awayGoals(1)
                .notes("Decided in the final minute")
                .canceledAt(null)
                .homeRegistration(homeReg)
                .awayRegistration(awayReg)
                .build();
    }

    @Test
    @DisplayName("toResponse should flatten competition, category, home, and away details")
    void toResponseShouldFlattenNestedAssociations() {
        var response = mapper.toResponse(aMatch());

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.round()).isEqualTo(3);
        assertThat(response.scheduledAt()).isEqualTo(LocalDateTime.of(2026, 6, 20, 19, 30));
        assertThat(response.venue()).isEqualTo("Municipal Arena");
        assertThat(response.status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(response.homeGoals()).isEqualTo(2);
        assertThat(response.awayGoals()).isEqualTo(1);
        assertThat(response.competitionId()).isEqualTo(100L);
        assertThat(response.competitionName()).isEqualTo("Municipal Cup");
        assertThat(response.categoryId()).isEqualTo(200L);
        assertThat(response.categoryName()).isEqualTo("U-20");
        assertThat(response.homeRegistrationId()).isEqualTo(10L);
        assertThat(response.homeTeamId()).isEqualTo(101L);
        assertThat(response.homeTeamName()).isEqualTo("Lions FC");
        assertThat(response.awayRegistrationId()).isEqualTo(20L);
        assertThat(response.awayTeamId()).isEqualTo(102L);
        assertThat(response.awayTeamName()).isEqualTo("Eagles FC");
        assertThat(response.notes()).isEqualTo("Decided in the final minute");
        assertThat(response.canceledAt()).isNull();
    }

    @Test
    @DisplayName("toSummary should map compact match fields")
    void toSummaryShouldMapCompactFields() {
        var summary = mapper.toSummary(aMatch());

        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.round()).isEqualTo(3);
        assertThat(summary.status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(summary.homeGoals()).isEqualTo(2);
        assertThat(summary.awayGoals()).isEqualTo(1);
        assertThat(summary.competitionId()).isEqualTo(100L);
        assertThat(summary.categoryId()).isEqualTo(200L);
        assertThat(summary.homeTeamId()).isEqualTo(101L);
        assertThat(summary.homeTeamName()).isEqualTo("Lions FC");
        assertThat(summary.awayTeamId()).isEqualTo(102L);
        assertThat(summary.awayTeamName()).isEqualTo("Eagles FC");
    }

    @Test
    @DisplayName("should return null when input is null")
    void nullInputsShouldReturnNull() {
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toSummary(null)).isNull();
    }
}
