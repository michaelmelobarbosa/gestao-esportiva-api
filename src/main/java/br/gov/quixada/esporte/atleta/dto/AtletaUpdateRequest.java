package br.gov.quixada.esporte.atleta.dto;

import br.gov.quixada.esporte.extras.EnderecoRequest;
import br.gov.quixada.esporte.extras.Sexo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record AtletaUpdateRequest(
        @NotBlank(message = "Nome completo é obrigatório")
        @Size(max=150)
        String nomeCompleto,
        @NotNull(message = "Data de nascimento é obrigatório")
        @Past(message = "A data de nascimento deve ser no passado")
        LocalDate dataNascimento,
        @NotNull
        @Valid
        EnderecoRequest endereco,
        @NotNull(message = "Sexo é obrigatório")
        Sexo sexo,
        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "\\d{10,12}", message = "Telefone deve conter apenas números e ter entre 10 e 12 dígitos")
        String telefone
) {
}
