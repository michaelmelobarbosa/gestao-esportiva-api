package br.gov.quixada.sports.competition.dto;

import br.gov.quixada.sports.competition.CompetitionStatus;
import jakarta.validation.constraints.NotNull;

public record CompetitionStatusRequest(
        @NotNull(message = "Status is required")
        CompetitionStatus status
) {
}
