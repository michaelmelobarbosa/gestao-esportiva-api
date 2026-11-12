package br.gov.quixada.sports.competition.dto;

import br.gov.quixada.sports.competition.CompetitionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CompetitionResponse(
        Long id,
        String name,
        Integer year,
        Long modalityId,
        String modalityName,
        LocalDate registrationStartDate,
        LocalDate registrationEndDate,
        LocalDate startDate,
        LocalDate endDate,
        CompetitionStatus status,
        String notes,
        LocalDateTime canceledAt
) {
}
