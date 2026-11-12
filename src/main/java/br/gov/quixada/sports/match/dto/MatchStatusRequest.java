package br.gov.quixada.sports.match.dto;

import br.gov.quixada.sports.match.MatchStatus;
import jakarta.validation.constraints.NotNull;

public record MatchStatusRequest(
        @NotNull(message = "Status is required")
        MatchStatus status
) {
}
