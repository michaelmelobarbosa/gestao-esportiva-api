package br.gov.quixada.sports.registration.dto;

import br.gov.quixada.sports.registration.RegistrationStatus;
import jakarta.validation.constraints.NotNull;

public record TeamRegistrationStatusRequest(
        @NotNull(message = "Status is required")
        RegistrationStatus status
) {
}
