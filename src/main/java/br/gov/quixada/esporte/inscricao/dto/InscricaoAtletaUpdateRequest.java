package br.gov.quixada.esporte.inscricao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InscricaoAtletaUpdateRequest(
        @NotBlank(message = "Número da camisa é obrigatório")
        @Size(max = 3, message = "Número da camisa deve ter no máximo 3 caracteres")
        @Pattern(regexp = "\\d{1,3}", message = "Número da camisa deve conter apenas números")
        String numeroCamisa
) {
}
