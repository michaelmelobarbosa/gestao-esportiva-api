package br.gov.quixada.sports.registration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import br.gov.quixada.sports.athlete.Athlete;
import br.gov.quixada.sports.athlete.AthleteStatus;
import br.gov.quixada.sports.registration.dto.AthleteRegistrationCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AthleteRegistrationMapper")
class AthleteRegistrationMapperTest {

    private AthleteRegistrationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AthleteRegistrationMapperImpl();
    }

    private AthleteRegistration aRegistration() {
        var athlete = Athlete.builder()
                .id(50L)
                .fullName("Francisco da Silva")
                .status(AthleteStatus.ACTIVE)
                .build();

        var teamRegistration = TeamRegistration.builder()
                .id(10L)
                .status(RegistrationStatus.APPROVED)
                .build();

        return AthleteRegistration.builder()
                .id(1L)
                .athlete(athlete)
                .teamRegistration(teamRegistration)
                .jerseyNumber("10")
                .registrationDate(LocalDateTime.of(2026, 1, 20, 11, 0))
                .status(RegistrationStatus.CANCELED)
                .notes("Injured before tournament")
                .canceledAt(LocalDateTime.of(2026, 2, 5, 16, 0))
                .build();
    }

    @Test
    @DisplayName("toEntity should map jerseyNumber and notes while ignoring associations and lifecycle fields")
    void toEntityShouldMapFieldsAndIgnoreManagedOnes() {
        var request = new AthleteRegistrationCreateRequest(10L, 50L, "10", "Captain");

        AthleteRegistration entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getAthlete()).isNull();
        assertThat(entity.getTeamRegistration()).isNull();
        assertThat(entity.getRegistrationDate()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getCanceledAt()).isNull();
        assertThat(entity.getJerseyNumber()).isEqualTo("10");
        assertThat(entity.getNotes()).isEqualTo("Captain");
    }

    @Test
    @DisplayName("toResponse should flatten athlete and teamRegistration fields")
    void toResponseShouldFlattenRelations() {
        var response = mapper.toResponse(aRegistration());

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.teamRegistrationId()).isEqualTo(10L);
        assertThat(response.athleteId()).isEqualTo(50L);
        assertThat(response.athleteName()).isEqualTo("Francisco da Silva");
        assertThat(response.jerseyNumber()).isEqualTo("10");
        assertThat(response.registrationDate()).isEqualTo(LocalDateTime.of(2026, 1, 20, 11, 0));
        assertThat(response.status()).isEqualTo(RegistrationStatus.CANCELED);
        assertThat(response.notes()).isEqualTo("Injured before tournament");
        assertThat(response.canceledAt()).isEqualTo(LocalDateTime.of(2026, 2, 5, 16, 0));
    }

    @Test
    @DisplayName("toSummary and toSummaryList should map summary fields")
    void toSummaryAndListShouldMapFields() {
        var registration = aRegistration();

        var summary = mapper.toSummary(registration);
        var list = mapper.toSummaryList(List.of(registration));

        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.teamRegistrationId()).isEqualTo(10L);
        assertThat(summary.athleteId()).isEqualTo(50L);
        assertThat(summary.athleteName()).isEqualTo("Francisco da Silva");
        assertThat(summary.jerseyNumber()).isEqualTo("10");
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
