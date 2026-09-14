package br.gov.quixada.esporte.modalidade.dto;

import br.gov.quixada.esporte.modalidade.StatusModalidade;
import jakarta.validation.constraints.NotNull;

public record ModalidadeStatusRequest(
        @NotNull(message = "Status é obrigatório")
        StatusModalidade status
) {
}
