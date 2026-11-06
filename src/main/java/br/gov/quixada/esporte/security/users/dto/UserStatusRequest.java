package br.gov.quixada.esporte.security.users.dto;

import br.gov.quixada.esporte.security.users.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull(message = "Status é obrigatório")
        UserStatus status
) {
}
