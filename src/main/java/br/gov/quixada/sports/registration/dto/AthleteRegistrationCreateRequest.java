package br.gov.quixada.sports.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AthleteRegistrationCreateRequest(
        @NotNull(message = "Team registration is required")
        Long teamRegistrationId,
        @NotNull(message = "Athlete is required")
        Long athleteId,
        @NotBlank(message = "Jersey number is required")
        @Size(max = 3, message = "Jersey number must have at most 3 digits")
        @Pattern(regexp = "\\d{1,3}", message = "Jersey number must contain only digits")
        String jerseyNumber,
        @Size(max = 500, message = "Notes must have at most 500 characters")
        String notes
) {
}
