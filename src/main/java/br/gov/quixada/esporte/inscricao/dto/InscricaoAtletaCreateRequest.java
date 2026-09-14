package br.gov.quixada.esporte.inscricao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InscricaoAtletaCreateRequest(
        @NotNull(message = "Inscrição de equipe é obrigatória")
        Long inscricaoEquipeId,
        @NotNull(message = "Atleta é obrigatório")
        Long atletaId,
        @NotBlank(message = "Número da camisa é obrigatório")
        @Size(max = 3, message = "Número da camisa deve ter no máximo 3 caracteres")
        @Pattern(regexp = "\\d{1,3}", message = "Número da camisa deve conter apenas números")
        String numeroCamisa
) {
}
