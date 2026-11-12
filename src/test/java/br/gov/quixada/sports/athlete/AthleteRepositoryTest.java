package br.gov.quixada.sports.athlete;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import br.gov.quixada.sports.shared.Address;
import br.gov.quixada.sports.shared.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@DisplayName("Athlete — persistence hooks")
class AthleteRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    private Athlete anAthlete(String fullName) {
        return Athlete.builder()
                .fullName(fullName)
                .cpf("52998224725")
                .birthDate(LocalDate.of(2000, 1, 1))
                .address(new Address("A Street", "10", "Downtown", "Quixadá", "63900-000"))
                .phone("88999999999")
                .gender(Gender.MALE)
                .build();
    }

    @Test
    @DisplayName("@PrePersist should populate normalizedFullName on insert")
    void prePersistShouldNormalizeName() {
        Athlete saved = entityManager.persistFlushFind(anAthlete("João Silva"));

        assertThat(saved.getNormalizedFullName()).isEqualTo("joaosilva");
    }

    @Test
    @DisplayName("@PreUpdate should renormalize normalizedFullName when the name is edited")
    void preUpdateShouldNormalizeNameOnEdit() {
        Athlete athlete = entityManager.persistFlushFind(anAthlete("João Silva"));

        athlete.updateDetails(
                "Maria Conceição",
                LocalDate.of(1990, 5, 5),
                new Address("B Street", "20", "Neighborhood Novo", "Fortaleza", "60000-000"),
                "88888888888",
                Gender.FEMALE
        );
        entityManager.flush();
        entityManager.clear();

        Athlete reloaded = entityManager.find(Athlete.class, athlete.getId());
        assertThat(reloaded.getNormalizedFullName()).isEqualTo("mariaconceicao");
    }
}
