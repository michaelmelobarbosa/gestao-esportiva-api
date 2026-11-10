package br.gov.quixada.esporte.partida.dto;

import br.gov.quixada.esporte.partida.StatusPartida;

import java.time.LocalDateTime;

public record PartidaResponse(
        Long id,
        Integer rodada,
        LocalDateTime dataHora,
        String local,
        StatusPartida status,
        Integer golsMandante,
        Integer golsVisitante,
        Long competicaoId,
        String competicaoNome,
        Long categoriaId,
        String categoriaNome,
        Long inscricaoMandanteId,
        Long mandanteEquipeId,
        String mandanteEquipeNome,
        Long inscricaoVisitanteId,
        Long visitanteEquipeId,
        String visitanteEquipeNome,
        String observacao,
        LocalDateTime dataCancelamento
) {
}
