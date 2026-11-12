package br.gov.quixada.sports.club.dto;

import br.gov.quixada.sports.club.ClubStatus;

public record ClubSummaryResponse(
        Long id,
        String name,
        String manager,
        String phone,
        ClubStatus status
) {
}
