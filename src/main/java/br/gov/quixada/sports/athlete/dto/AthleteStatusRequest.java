package br.gov.quixada.sports.athlete.dto;

import br.gov.quixada.sports.athlete.AthleteStatus;
import jakarta.validation.constraints.NotNull;

public record AthleteStatusRequest(
        @NotNull(message = "Status is required")
        AthleteStatus status
) {
}
