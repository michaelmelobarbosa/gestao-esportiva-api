package br.gov.quixada.sports.category.dto;

import br.gov.quixada.sports.category.CategoryStatus;
import jakarta.validation.constraints.NotNull;

public record CategoryStatusRequest(
        @NotNull(message = "Status is required")
        CategoryStatus status
) {
}
