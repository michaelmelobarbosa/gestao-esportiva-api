package br.gov.quixada.sports.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AthleteRegistrationUpdateRequest(
        @NotBlank(message = "Jersey number is required")
        @Size(max = 3, message = "Jersey number must have at most 3 characters")
        @Pattern(regexp = "\\d{1,3}", message = "Jersey number must contain only digits")
        String jerseyNumber
) {
}
