package br.gov.quixada.esporte.inscricao.dto;

import br.gov.quixada.esporte.inscricao.StatusInscricao;

public record InscricaoAtletaResumoResponse(
        Long id,
        Long inscricaoEquipeId,
        Long atletaId,
        String atletaNome,
        String numeroCamisa,
        StatusInscricao status
) {
}
