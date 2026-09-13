package br.gov.quixada.esporte.competicao.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CompeticaoUpdateRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String nome,
        @NotNull(message = "Ano é obrigatório")
        @Positive(message = "Ano deve ser positivo")
        @Min(value = 1900, message = "Ano deve ser maior ou igual a 1900")
        Integer ano,
        @NotNull(message = "Data de início é obrigatória")
        LocalDate dataInicio,
        @NotNull(message = "Data de fim é obrigatória")
        LocalDate dataFim
) {
}
