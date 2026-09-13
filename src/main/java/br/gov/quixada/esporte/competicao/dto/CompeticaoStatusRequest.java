package br.gov.quixada.esporte.competicao.dto;

import br.gov.quixada.esporte.competicao.StatusCompeticao;
import jakarta.validation.constraints.NotNull;

public record CompeticaoStatusRequest(
        @NotNull(message = "Status é obrigatório")
        StatusCompeticao status
) {
}
