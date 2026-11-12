package br.gov.quixada.sports.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must have at most 100 characters")
        String name,
        @NotNull(message = "Min age is required")
        @PositiveOrZero(message = "Min age must be zero or positive")
        Integer minAge,
        @NotNull(message = "Max age is required")
        @PositiveOrZero(message = "Max age must be zero or positive")
        Integer maxAge
) {
}
