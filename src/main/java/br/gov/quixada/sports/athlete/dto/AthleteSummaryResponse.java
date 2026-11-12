package br.gov.quixada.sports.athlete.dto;

import br.gov.quixada.sports.shared.Gender;
import br.gov.quixada.sports.athlete.AthleteStatus;

public record AthleteSummaryResponse(
        Long id,
        String fullName,
        String cpf,
        Integer age,
        String phone,
        Gender gender,
        AthleteStatus status
) {
}
