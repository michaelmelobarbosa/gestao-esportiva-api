package br.gov.quixada.sports.registration.dto;

import br.gov.quixada.sports.registration.RegistrationStatus;

public record AthleteRegistrationSummaryResponse(
        Long id,
        Long teamRegistrationId,
        Long athleteId,
        String athleteName,
        String jerseyNumber,
        RegistrationStatus status
) {
}
