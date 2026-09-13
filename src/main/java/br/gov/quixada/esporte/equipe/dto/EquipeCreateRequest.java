package br.gov.quixada.esporte.equipe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EquipeCreateRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String nome,
        @NotNull(message = "Clube é obrigatório")
        Long clubeId,
        @NotNull(message = "Modalidade é obrigatória")
        Long modalidadeId,
        @NotBlank(message = "Responsável é obrigatório")
        @Size(max = 100, message = "Responsável deve ter no máximo 100 caracteres")
        String responsavel
) {
}
