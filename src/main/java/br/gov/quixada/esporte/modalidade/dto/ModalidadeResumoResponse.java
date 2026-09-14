package br.gov.quixada.esporte.modalidade.dto;

import br.gov.quixada.esporte.modalidade.StatusModalidade;

public record ModalidadeResumoResponse(
        Long id,
        String nome,
        StatusModalidade status
) {
}
