package br.gov.quixada.esporte.inscricao.dto;

import br.gov.quixada.esporte.inscricao.StatusInscricao;

public record InscricaoEquipeResumoResponse(
        Long id,
        Long competicaoId,
        String competicaoNome,
        Long categoriaId,
        String categoriaNome,
        Long equipeId,
        String equipeNome,
        StatusInscricao status
) {
}
