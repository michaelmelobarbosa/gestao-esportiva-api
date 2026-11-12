package br.gov.quixada.sports.registration.dto;

import br.gov.quixada.sports.registration.RegistrationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record TeamRegistrationResponse(
        Long id,
        Long competitionId,
        String competitionName,
        Long categoryId,
        String categoryName,
        Long teamId,
        String teamName,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime registrationDate,
        RegistrationStatus status,
        String notes,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime canceledAt
) {
}
