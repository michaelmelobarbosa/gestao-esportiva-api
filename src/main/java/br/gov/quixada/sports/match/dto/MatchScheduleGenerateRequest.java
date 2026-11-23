package br.gov.quixada.sports.match.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record MatchScheduleGenerateRequest(
        boolean doubleRoundRobin,
        LocalDateTime firstRoundAt,
        @Positive(message = "Days between rounds must be positive")
        Integer daysBetweenRounds,
        @Size(max = 150, message = "Venue must have at most 150 characters")
        String defaultVenue
) {
}
