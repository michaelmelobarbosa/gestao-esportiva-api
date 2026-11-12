package br.gov.quixada.sports.athlete;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import br.gov.quixada.sports.athlete.dto.AthleteUpdateRequest;
import br.gov.quixada.sports.shared.Address;
import br.gov.quixada.sports.shared.AddressMapperImpl;
import br.gov.quixada.sports.shared.AddressRequest;
import br.gov.quixada.sports.shared.Gender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("AthleteMapper")
class AthleteMapperTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Fortaleza"));

    private AthleteMapper mapper;

    @BeforeEach
    void setUp() {
        var impl = new AthleteMapperImpl();
        ReflectionTestUtils.setField(impl, "addressMapper", new AddressMapperImpl());
        mapper = impl;
    }

    private Athlete anAthlete(LocalDate birthDate) {
        return Athlete.builder()
                .id(1L)
                .fullName("Francisco da Silva")
                .cpf("52998224725")
                .birthDate(birthDate)
                .address(new Address("A Street", "10", "Downtown", "Quixadá", "63900-000"))
                .phone("88999999999")
                .gender(Gender.MALE)
                .status(AthleteStatus.ACTIVE)
                .build();
    }

    private AthleteUpdateRequest anUpdateRequest() {
        return new AthleteUpdateRequest(
                "Maria Souza",
                LocalDate.of(1990, 5, 5),
                new AddressRequest("B Street", "20", "Neighborhood Novo", "Fortaleza", "60000-000"),
                Gender.FEMALE,
                "88888888888"
        );
    }

    @Test
    @DisplayName("calculateAge should use the provided Clock")
    void calculateAgeShouldUseClock() {
        assertThat(mapper.calculateAge(LocalDate.of(2000, 6, 15), CLOCK)).isEqualTo(26);
    }

    @Test
    @DisplayName("calculateAge should consider a birthday that has not happened yet")
    void calculateAgeBeforeBirthday() {
        assertThat(mapper.calculateAge(LocalDate.of(2000, 6, 16), CLOCK)).isEqualTo(25);
    }

    @Test
    @DisplayName("calculateAge should return null when the date is null")
    void calculateAgeWithNullDate() {
        assertThat(mapper.calculateAge(null, CLOCK)).isNull();
    }

    @Test
    @DisplayName("toEntity(AthleteUpdateRequest) should not map the CPF")
    void updateShouldNotMapCpf() {
        Athlete athlete = mapper.toEntity(anUpdateRequest());

        assertThat(athlete.getCpf()).isNull();
        assertThat(athlete.getFullName()).isEqualTo("Maria Souza");
        assertThat(athlete.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(athlete.getPhone()).isEqualTo("88888888888");
        assertThat(athlete.getAddress().getCity()).isEqualTo("Fortaleza");
        assertThat(athlete.getStatus()).isNull();
    }

    @Test
    @DisplayName("toEntity(AthleteUpdateRequest) should not map id nor created date")
    void updateShouldNotMapCreationFields() {
        Athlete athlete = mapper.toEntity(anUpdateRequest());

        assertThat(athlete.getId()).isNull();
        assertThat(athlete.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("toGetResponse should calculate the age with the Clock")
    void toGetResponseShouldCalculateAge() {
        var response = mapper.toGetResponse(anAthlete(LocalDate.of(2000, 6, 15)), CLOCK);

        assertThat(response.age()).isEqualTo(26);
        assertThat(response.fullName()).isEqualTo("Francisco da Silva");
        assertThat(response.address().city()).isEqualTo("Quixadá");
    }

    @Test
    @DisplayName("toGetResponse should return null age when birthDate is null")
    void toGetResponseWithNullBirthDate() {
        var response = mapper.toGetResponse(anAthlete(null), CLOCK);

        assertThat(response.birthDate()).isNull();
        assertThat(response.age()).isNull();
    }

    @Test
    @DisplayName("toSummary should return null age when birthDate is null")
    void toSummaryWithNullBirthDate() {
        var response = mapper.toSummary(anAthlete(null), CLOCK);

        assertThat(response.age()).isNull();
        assertThat(response.fullName()).isEqualTo("Francisco da Silva");
    }

    @Test
    @DisplayName("toSummary should calculate the age with the Clock")
    void toSummaryShouldCalculateAge() {
        var response = mapper.toSummary(anAthlete(LocalDate.of(2000, 6, 16)), CLOCK);

        assertThat(response.age()).isEqualTo(25);
    }
}
