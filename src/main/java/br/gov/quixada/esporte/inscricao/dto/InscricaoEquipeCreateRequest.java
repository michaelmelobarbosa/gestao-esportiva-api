package br.gov.quixada.esporte.inscricao.dto;

import jakarta.validation.constraints.NotNull;

public record InscricaoEquipeCreateRequest(
        @NotNull(message = "Competição é obrigatória")
        Long competicaoId,
        @NotNull(message = "Categoria é obrigatória")
        Long categoriaId,
        @NotNull(message = "Equipe é obrigatória")
        Long equipeId
) {
}
