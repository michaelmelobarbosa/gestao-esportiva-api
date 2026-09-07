package br.gov.quixada.esporte.atleta.dto;

import br.gov.quixada.esporte.extras.EnderecoRequest;
import br.gov.quixada.esporte.extras.Sexo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AtletaUpdateRequest(
        @NotBlank(message = "Nome completo é obrigatório")
        @Size(max=150)
        String nomeCompleto,
        @NotNull(message = "Data de nascimento é obrigatório")
        LocalDate dataNascimento,
        @NotNull
        @Valid
        EnderecoRequest endereco,
        @NotNull(message = "Sexo é obrigatório")
        Sexo sexo,
        @NotBlank(message = "Telefone é obrigatório")
        @Size(max=20)
        String telefone
) {
}
