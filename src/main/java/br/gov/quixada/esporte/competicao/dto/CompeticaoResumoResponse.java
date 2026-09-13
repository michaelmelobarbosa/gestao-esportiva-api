package br.gov.quixada.esporte.competicao.dto;

import br.gov.quixada.esporte.competicao.StatusCompeticao;

import java.time.LocalDate;

public record CompeticaoResumoResponse(
        Long id,
        String nome,
        Integer ano,
        Long modalidadeId,
        String modalidadeNome,
        LocalDate dataInicio,
        LocalDate dataFim,
        StatusCompeticao status
) {
}
