package br.gov.quixada.esporte.atleta.dto;

import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.extras.Sexo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AtletaUpdateRequest(
        @NotBlank(message = "Nome completo é obrigatório")
        String nomeCompleto,
        @NotBlank(message = "Data de nascimento é obrigatório")
        @NotNull
        LocalDate dataNascimento,
        @NotBlank(message = "endereço é obrigatório")
        @NotNull
        Sexo sexo,
        Endereco endereco,
        @NotBlank(message = "Telefone é obrigatório")
        String telefone
) {
}
