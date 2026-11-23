package br.gov.quixada.sports.category.dto;

import br.gov.quixada.sports.category.CategoryStatus;

public record CategoryResponse(
        Long id,
        String name,
        Integer minAge,
        Integer maxAge,
        Integer minAthletes,
        Integer maxAthletes,
        Long competitionId,
        String competitionName,
        CategoryStatus status
) {
}
