package br.gov.quixada.sports.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamUpdateRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must have at most 100 characters")
        String name,
        @NotBlank(message = "Manager is required")
        @Size(max = 100, message = "Manager must have at most 100 characters")
        String manager
) {
}
