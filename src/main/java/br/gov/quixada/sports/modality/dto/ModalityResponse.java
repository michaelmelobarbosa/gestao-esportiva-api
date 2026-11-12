package br.gov.quixada.sports.modality.dto;

import br.gov.quixada.sports.modality.ModalityStatus;

public record ModalityResponse(
        Long id,
        String name,
        ModalityStatus status
) {
}
