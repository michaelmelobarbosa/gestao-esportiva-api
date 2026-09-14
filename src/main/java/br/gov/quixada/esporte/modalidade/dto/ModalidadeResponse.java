package br.gov.quixada.esporte.modalidade.dto;

import br.gov.quixada.esporte.modalidade.StatusModalidade;

public record ModalidadeResponse(
        Long id,
        String nome,
        StatusModalidade status
) {
}
