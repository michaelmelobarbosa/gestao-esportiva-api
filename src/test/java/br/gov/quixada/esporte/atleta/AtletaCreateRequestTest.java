package br.gov.quixada.esporte.atleta;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.gov.quixada.esporte.atleta.dto.AtletaCreateRequest;
import br.gov.quixada.esporte.extras.EnderecoRequest;
import br.gov.quixada.esporte.extras.Sexo;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@DisplayName("AtletaCreateRequest — CPF")
class AtletaCreateRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private AtletaCreateRequest request(String cpf) {
        return new AtletaCreateRequest(
                "Fulano de Tal",
                cpf,
                LocalDate.of(2000, 1, 1),
                new EnderecoRequest("Rua A", "10", "Centro", "Quixadá", "63900-000"),
                Sexo.MASCULINO,
                "88999999999"
        );
    }

    @Test
    @DisplayName("deve aceitar CPF válido com máscara")
    void deveAceitarCpfComMascara() {
        assertThat(validator.validate(request("529.982.247-25"))).isEmpty();
    }

    @Test
    @DisplayName("deve aceitar CPF válido sem máscara")
    void deveAceitarCpfSemMascara() {
        assertThat(validator.validate(request("52998224725"))).isEmpty();
    }

    @Test
    @DisplayName("deve rejeitar CPF inválido")
    void deveRejeitarCpfInvalido() {
        var violacoes = validator.validate(request("111.111.111-11"));

        assertThat(violacoes).anySatisfy(violacao -> {
            assertThat(violacao.getPropertyPath().toString()).isEqualTo("cpf");
            assertThat(violacao.getMessage()).isEqualTo("CPF inválido");
        });
    }
}
