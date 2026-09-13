package br.gov.quixada.esporte.equipe.dto;

import br.gov.quixada.esporte.equipe.StatusEquipe;

public record EquipeResumoResponse(
        Long id,
        String nome,
        Long clubeId,
        String clubeNome,
        Long modalidadeId,
        String modalidadeNome,
        String responsavel,
        StatusEquipe status
) {
}
