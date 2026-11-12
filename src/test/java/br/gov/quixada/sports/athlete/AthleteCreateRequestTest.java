package br.gov.quixada.sports.athlete;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.gov.quixada.sports.athlete.dto.AthleteCreateRequest;
import br.gov.quixada.sports.shared.AddressRequest;
import br.gov.quixada.sports.shared.Gender;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@DisplayName("AthleteCreateRequest — CPF")
class AthleteCreateRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private AthleteCreateRequest request(String cpf) {
        return new AthleteCreateRequest(
                "John de Tal",
                cpf,
                LocalDate.of(2000, 1, 1),
                new AddressRequest("A Street", "10", "Downtown", "Quixadá", "63900-000"),
                Gender.MALE,
                "88999999999"
        );
    }

    @Test
    @DisplayName("should accept a valid CPF with a mask")
    void shouldAcceptCpfWithMask() {
        assertThat(validator.validate(request("529.982.247-25"))).isEmpty();
    }

    @Test
    @DisplayName("should accept a valid CPF without a mask")
    void shouldAcceptCpfWithoutMask() {
        assertThat(validator.validate(request("52998224725"))).isEmpty();
    }

    @Test
    @DisplayName("should reject an invalid CPF")
    void shouldRejectInvalidCpf() {
        var violations = validator.validate(request("111.111.111-11"));

        assertThat(violations).anySatisfy(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("cpf");
            assertThat(violation.getMessage()).isEqualTo("Invalid CPF");
        });
    }
}
