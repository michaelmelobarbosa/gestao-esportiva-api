package br.gov.quixada.sports.registration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import br.gov.quixada.sports.category.Category;
import br.gov.quixada.sports.category.CategoryStatus;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionStatus;
import br.gov.quixada.sports.registration.dto.TeamRegistrationCreateRequest;
import br.gov.quixada.sports.team.Team;
import br.gov.quixada.sports.team.TeamStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TeamRegistrationMapper")
class TeamRegistrationMapperTest {

    private TeamRegistrationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TeamRegistrationMapperImpl();
    }

    private TeamRegistration aRegistration() {
        var competition = Competition.builder()
                .id(100L)
                .name("Municipal Cup")
                .status(CompetitionStatus.REGISTRATION_OPEN)
                .build();

        var category = Category.builder()
                .id(200L)
                .name("U-20")
                .competition(competition)
                .status(CategoryStatus.ACTIVE)
                .build();

        var team = Team.builder()
                .id(300L)
                .name("Lions FC")
                .status(TeamStatus.ACTIVE)
                .build();

        return TeamRegistration.builder()
                .id(1L)
                .competition(competition)
                .category(category)
                .team(team)
                .registrationDate(LocalDateTime.of(2026, 1, 15, 10, 0))
                .status(RegistrationStatus.CANCELED)
                .notes("Withdrawn by manager")
                .canceledAt(LocalDateTime.of(2026, 2, 1, 9, 30))
                .build();
    }

    @Test
    @DisplayName("toEntity should map notes and ignore associations and lifecycle fields")
    void toEntityShouldMapNotesAndIgnoreManagedFields() {
        var request = new TeamRegistrationCreateRequest(100L, 200L, 300L, "Initial note");

        TeamRegistration entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getCompetition()).isNull();
        assertThat(entity.getCategory()).isNull();
        assertThat(entity.getTeam()).isNull();
        assertThat(entity.getRegistrationDate()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getCanceledAt()).isNull();
        assertThat(entity.getNotes()).isEqualTo("Initial note");
    }

    @Test
    @DisplayName("toResponse should flatten competition, category, and team fields")
    void toResponseShouldFlattenRelations() {
        var response = mapper.toResponse(aRegistration());

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.competitionId()).isEqualTo(100L);
        assertThat(response.competitionName()).isEqualTo("Municipal Cup");
        assertThat(response.categoryId()).isEqualTo(200L);
        assertThat(response.categoryName()).isEqualTo("U-20");
        assertThat(response.teamId()).isEqualTo(300L);
        assertThat(response.teamName()).isEqualTo("Lions FC");
        assertThat(response.registrationDate()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 0));
        assertThat(response.status()).isEqualTo(RegistrationStatus.CANCELED);
        assertThat(response.notes()).isEqualTo("Withdrawn by manager");
        assertThat(response.canceledAt()).isEqualTo(LocalDateTime.of(2026, 2, 1, 9, 30));
    }

    @Test
    @DisplayName("toSummary and toSummaryList should map summary fields")
    void toSummaryAndListShouldMapFields() {
        var registration = aRegistration();

        var summary = mapper.toSummary(registration);
        var list = mapper.toSummaryList(List.of(registration));

        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.competitionId()).isEqualTo(100L);
        assertThat(summary.competitionName()).isEqualTo("Municipal Cup");
        assertThat(summary.categoryId()).isEqualTo(200L);
        assertThat(summary.categoryName()).isEqualTo("U-20");
        assertThat(summary.teamId()).isEqualTo(300L);
        assertThat(summary.teamName()).isEqualTo("Lions FC");
        assertThat(summary.status()).isEqualTo(RegistrationStatus.CANCELED);
        assertThat(list).containsExactly(summary);
    }

    @Test
    @DisplayName("should return null when input is null")
    void nullInputsShouldReturnNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toSummary(null)).isNull();
        assertThat(mapper.toSummaryList(null)).isNull();
    }
}
