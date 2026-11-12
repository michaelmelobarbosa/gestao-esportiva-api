package br.gov.quixada.sports.registration.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeamRegistrationCreateRequest(
        @NotNull(message = "Competition is required")
        Long competitionId,
        @NotNull(message = "Category is required")
        Long categoryId,
        @NotNull(message = "Team is required")
        Long teamId,
        @Size(max = 500, message = "Notes must have at most 500 characters")
        String notes
) {
}
