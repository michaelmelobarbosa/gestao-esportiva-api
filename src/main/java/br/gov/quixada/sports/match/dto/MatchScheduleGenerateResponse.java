package br.gov.quixada.sports.match.dto;

import java.util.List;

public record MatchScheduleGenerateResponse(
        int totalRounds,
        int totalMatches,
        List<MatchSummaryResponse> matches
) {
}
