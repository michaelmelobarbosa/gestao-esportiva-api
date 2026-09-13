package br.gov.quixada.esporte.atleta.dto;

import br.gov.quixada.esporte.atleta.StatusAtleta;
import jakarta.validation.constraints.NotNull;

public record AtletaStatusRequest(
        @NotNull(message = "Status é obrigatório")
        StatusAtleta status
) {
}
