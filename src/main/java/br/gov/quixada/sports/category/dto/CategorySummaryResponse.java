package br.gov.quixada.sports.category.dto;

import br.gov.quixada.sports.category.CategoryStatus;

public record CategorySummaryResponse(
        Long id,
        String name,
        Integer minAge,
        Integer maxAge,
        Long competitionId,
        CategoryStatus status
) {
}
