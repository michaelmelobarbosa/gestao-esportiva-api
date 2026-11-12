package br.gov.quixada.sports.registration.dto;

import br.gov.quixada.sports.registration.RegistrationStatus;

public record TeamRegistrationSummaryResponse(
        Long id,
        Long competitionId,
        String competitionName,
        Long categoryId,
        String categoryName,
        Long teamId,
        String teamName,
        RegistrationStatus status
) {
}
