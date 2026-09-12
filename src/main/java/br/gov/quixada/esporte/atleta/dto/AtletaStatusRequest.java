package br.gov.quixada.esporte.atleta.dto;

import br.gov.quixada.esporte.extras.StatusAtleta;
import jakarta.validation.constraints.NotNull;

public record AtletaStatusRequest(
        @NotNull
        StatusAtleta status
) {
}
