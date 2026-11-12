package br.gov.quixada.sports.match.dto;

import br.gov.quixada.sports.match.MatchStatus;

import java.time.LocalDateTime;

public record MatchResponse(
        Long id,
        Integer round,
        LocalDateTime scheduledAt,
        String venue,
        MatchStatus status,
        Integer homeGoals,
        Integer awayGoals,
        Long competitionId,
        String competitionName,
        Long categoryId,
        String categoryName,
        Long homeRegistrationId,
        Long homeTeamId,
        String homeTeamName,
        Long awayRegistrationId,
        Long awayTeamId,
        String awayTeamName,
        String notes,
        LocalDateTime canceledAt
) {
}
