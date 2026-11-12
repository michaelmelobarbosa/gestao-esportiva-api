package br.gov.quixada.sports.match.dto;

import br.gov.quixada.sports.match.MatchStatus;

import java.time.LocalDateTime;

public record MatchSummaryResponse(
        Long id,
        Integer round,
        LocalDateTime scheduledAt,
        String venue,
        MatchStatus status,
        Integer homeGoals,
        Integer awayGoals,
        Long competitionId,
        Long categoryId,
        Long homeTeamId,
        String homeTeamName,
        Long awayTeamId,
        String awayTeamName
) {
}
