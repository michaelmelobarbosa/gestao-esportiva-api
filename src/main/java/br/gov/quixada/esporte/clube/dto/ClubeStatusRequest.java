package br.gov.quixada.esporte.clube.dto;

import br.gov.quixada.esporte.extras.StatusClube;
import jakarta.validation.constraints.NotNull;

public record ClubeStatusRequest(
        @NotNull(message = "Status é obrigatório")
        StatusClube status
) {
}
