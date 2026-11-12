package br.gov.quixada.sports.team.dto;

import br.gov.quixada.sports.team.TeamStatus;

public record TeamSummaryResponse(
        Long id,
        String name,
        Long clubId,
        String clubName,
        Long modalityId,
        String modalityName,
        String manager,
        TeamStatus status
) {
}
