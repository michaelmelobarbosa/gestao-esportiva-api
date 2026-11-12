package br.gov.quixada.sports.team.dto;

import br.gov.quixada.sports.team.TeamStatus;
import jakarta.validation.constraints.NotNull;

public record TeamStatusRequest(
        @NotNull(message = "Status is required")
        TeamStatus status
) {
}
