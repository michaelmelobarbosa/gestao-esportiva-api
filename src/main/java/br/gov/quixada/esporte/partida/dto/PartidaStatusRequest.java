package br.gov.quixada.esporte.partida.dto;

import br.gov.quixada.esporte.partida.StatusPartida;
import jakarta.validation.constraints.NotNull;

public record PartidaStatusRequest(
        @NotNull(message = "Status é obrigatório")
        StatusPartida status
) {
}
