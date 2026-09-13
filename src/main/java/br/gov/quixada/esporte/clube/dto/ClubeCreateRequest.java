package br.gov.quixada.esporte.clube.dto;

import br.gov.quixada.esporte.extras.EnderecoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClubeCreateRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String nome,
        @NotBlank(message = "Responsável é obrigatório")
        @Size(max = 100, message = "Responsável deve ter no máximo 100 caracteres")
        String responsavel,
        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "\\d{10,12}", message = "Telefone deve conter apenas números e ter entre 10 e 12 dígitos")
        String telefone,
        @NotNull(message = "Endereço é obrigatório")
        @Valid
        EnderecoRequest endereco
) {
}
