package br.gov.quixada.esporte.partida.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PartidaResultadoRequest(
        @NotNull(message = "Gols do mandante são obrigatórios")
        @PositiveOrZero(message = "Gols do mandante não pode ser negativo")
        Integer golsMandante,
        @NotNull(message = "Gols do visitante são obrigatórios")
        @PositiveOrZero(message = "Gols do visitante não pode ser negativo")
        Integer golsVisitante
) {
}
