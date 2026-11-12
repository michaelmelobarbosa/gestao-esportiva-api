package br.gov.quixada.sports.competition.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CompetitionCreateRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must have at most 100 characters")
        String name,
        @NotNull(message = "Year is required")
        @Positive(message = "Year must be positive")
        @Min(value = 1900, message = "Year must be greater than or equal to 1900")
        Integer year,
        @NotNull(message = "Modality is required")
        Long modalityId,
        @NotNull(message = "Registration start date is required")
        LocalDate registrationStartDate,
        @NotNull(message = "Registration end date is required")
        LocalDate registrationEndDate,
        @NotNull(message = "Start date is required")
        LocalDate startDate,
        @NotNull(message = "End date is required")
        LocalDate endDate,
        @Size(max = 500, message = "Notes must have at most 500 characters")
        String notes
) {
}
