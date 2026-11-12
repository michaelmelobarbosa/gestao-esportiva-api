package br.gov.quixada.sports.modality.dto;

import br.gov.quixada.sports.modality.ModalityStatus;

public record ModalitySummaryResponse(
        Long id,
        String name,
        ModalityStatus status
) {
}
