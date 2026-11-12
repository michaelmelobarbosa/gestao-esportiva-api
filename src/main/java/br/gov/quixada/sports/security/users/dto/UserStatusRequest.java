package br.gov.quixada.sports.security.users.dto;

import br.gov.quixada.sports.security.users.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull(message = "Status is required")
        UserStatus status
) {
}
