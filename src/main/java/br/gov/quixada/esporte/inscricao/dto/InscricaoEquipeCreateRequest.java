package br.gov.quixada.esporte.inscricao.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InscricaoEquipeCreateRequest(
        @NotNull(message = "Competição é obrigatória")
        Long competicaoId,
        @NotNull(message = "Categoria é obrigatória")
        Long categoriaId,
        @NotNull(message = "Equipe é obrigatória")
        Long equipeId,
        @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
        String observacao
) {
}
