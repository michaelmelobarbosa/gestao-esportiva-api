package br.gov.quixada.sports.match.dto;

public record StandingsResponse(
        Integer position,
        Long teamRegistrationId,
        Long teamId,
        String teamName,
        Integer points,
        Integer played,
        Integer wins,
        Integer draws,
        Integer losses,
        Integer goalsFor,
        Integer goalsAgainst,
        Integer goalDifference,
        Double pointsPercentage
) {
}
