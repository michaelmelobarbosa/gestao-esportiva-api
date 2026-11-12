package br.gov.quixada.sports.match.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record MatchCreateRequest(
        @NotNull(message = "Home registration is required")
        Long homeRegistrationId,
        @NotNull(message = "Away registration is required")
        Long awayRegistrationId,
        @NotNull(message = "Round is required")
        @Positive(message = "Round must be positive")
        Integer round,
        @NotNull(message = "Date and time are required")
        LocalDateTime scheduledAt,
        @Size(max = 150, message = "Venue must have at most 150 characters")
        String venue,
        @Size(max = 500, message = "Notes must have at most 500 characters")
        String notes
) {
}
