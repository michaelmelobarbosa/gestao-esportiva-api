package br.gov.quixada.esporte.partida.dto;

import br.gov.quixada.esporte.partida.StatusPartida;

import java.time.LocalDateTime;

public record PartidaResumoResponse(
        Long id,
        Integer rodada,
        LocalDateTime dataHora,
        String local,
        StatusPartida status,
        Integer golsMandante,
        Integer golsVisitante,
        Long competicaoId,
        Long categoriaId,
        Long mandanteEquipeId,
        String mandanteEquipeNome,
        Long visitanteEquipeId,
        String visitanteEquipeNome
) {
}
