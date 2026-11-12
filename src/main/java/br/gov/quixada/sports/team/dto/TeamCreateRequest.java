package br.gov.quixada.sports.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeamCreateRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must have at most 100 characters")
        String name,
        @NotNull(message = "Club is required")
        Long clubId,
        @NotNull(message = "Modality is required")
        Long modalityId,
        @NotBlank(message = "Manager is required")
        @Size(max = 100, message = "Manager must have at most 100 characters")
        String manager
) {
}
