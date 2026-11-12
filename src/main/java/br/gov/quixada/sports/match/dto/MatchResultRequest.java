package br.gov.quixada.sports.match.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MatchResultRequest(
        @NotNull(message = "Home goals are required")
        @PositiveOrZero(message = "Home goals cannot be negative")
        Integer homeGoals,
        @NotNull(message = "Away goals are required")
        @PositiveOrZero(message = "Away goals cannot be negative")
        Integer awayGoals
) {
}
