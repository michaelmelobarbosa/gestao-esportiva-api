package br.gov.quixada.sports.athlete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import br.gov.quixada.sports.category.Category;
import br.gov.quixada.sports.club.Club;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionStatus;
import br.gov.quixada.sports.team.Team;
import br.gov.quixada.sports.shared.Address;
import br.gov.quixada.sports.shared.Gender;
import br.gov.quixada.sports.registration.AthleteRegistration;
import br.gov.quixada.sports.registration.AthleteRegistrationRepository;
import br.gov.quixada.sports.registration.TeamRegistration;
import br.gov.quixada.sports.registration.RegistrationStatus;
import br.gov.quixada.sports.modality.Modality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@DisplayName("Athlete — hard delete and referential integrity")
class AthleteHardDeleteRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private AthleteRegistrationRepository athleteRegistrationRepository;

    private Athlete anAthlete(String name) {
        return Athlete.builder()
                .fullName(name)
                .cpf("52998224725")
                .birthDate(LocalDate.of(2000, 1, 1))
                .address(new Address("A Street", "10", "Downtown", "Quixadá", "63900-000"))
                .phone("88999999999")
                .gender(Gender.MALE)
                .status(AthleteStatus.INACTIVE)
                .build();
    }

    private AthleteRegistration aRegistration(Athlete athlete, RegistrationStatus status) {
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

        return entityManager.persistFlushFind(AthleteRegistration.builder()
                .athlete(athlete)
                .teamRegistration(teamRegistration)
                .jerseyNumber("10")
                .status(status)
                .build());
    }

    @Test
    @DisplayName("should not allow deleting an athlete while a linked registration exists")
    void shouldNotDeleteAthleteWithRegistration() {
        Athlete athlete = entityManager.persistFlushFind(anAthlete("João Silva"));
        aRegistration(athlete, RegistrationStatus.CANCELED);
        entityManager.clear();

        Athlete reloaded = athleteRepository.findById(athlete.getId()).orElseThrow();
        athleteRepository.delete(reloaded);

        assertThatThrownBy(() -> athleteRepository.flush())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("deleteByAthleteIdAndStatusIn should remove registrations that free the slot")
    void shouldRemoveRegistrationsThatFreeTheSlot() {
        Athlete athlete = entityManager.persistFlushFind(anAthlete("Maria Souza"));
        aRegistration(athlete, RegistrationStatus.CANCELED);
        aRegistration(athlete, RegistrationStatus.REJECTED);

        long removed = athleteRegistrationRepository.deleteByAthleteIdAndStatusIn(
                athlete.getId(), RegistrationStatus.FREE_SLOT);
        entityManager.flush();
        athleteRepository.delete(athlete);
        athleteRepository.flush();

        assertThat(removed).isEqualTo(2);
        assertThat(athleteRepository.findById(athlete.getId())).isEmpty();
    }
}
