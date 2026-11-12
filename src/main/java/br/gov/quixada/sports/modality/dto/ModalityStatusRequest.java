package br.gov.quixada.sports.modality.dto;

import br.gov.quixada.sports.modality.ModalityStatus;
import jakarta.validation.constraints.NotNull;

public record ModalityStatusRequest(
        @NotNull(message = "Status is required")
        ModalityStatus status
) {
}
