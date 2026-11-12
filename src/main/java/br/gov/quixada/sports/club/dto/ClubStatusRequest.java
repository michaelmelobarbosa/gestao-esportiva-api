package br.gov.quixada.sports.club.dto;

import br.gov.quixada.sports.club.ClubStatus;
import jakarta.validation.constraints.NotNull;

public record ClubStatusRequest(
        @NotNull(message = "Status is required")
        ClubStatus status
) {
}
