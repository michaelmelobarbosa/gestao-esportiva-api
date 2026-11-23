package br.gov.quixada.sports.category.dto;

import br.gov.quixada.sports.category.CategoryStatus;

public record CategorySummaryResponse(
        Long id,
        String name,
        Integer minAge,
        Integer maxAge,
        Integer minAthletes,
        Integer maxAthletes,
        Long competitionId,
        CategoryStatus status
) {
}
