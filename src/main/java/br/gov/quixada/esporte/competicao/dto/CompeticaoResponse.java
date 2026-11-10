package br.gov.quixada.esporte.competicao.dto;

import br.gov.quixada.esporte.competicao.StatusCompeticao;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CompeticaoResponse(
        Long id,
        String nome,
        Integer ano,
        Long modalidadeId,
        String modalidadeNome,
        LocalDate dataInicioInscricao,
        LocalDate dataFimInscricao,
        LocalDate dataInicio,
        LocalDate dataFim,
        StatusCompeticao status,
        String observacao,
        LocalDateTime dataCancelamento
) {
}
