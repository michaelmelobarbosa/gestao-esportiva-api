package br.gov.quixada.sports.registration.dto;

import br.gov.quixada.sports.registration.RegistrationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record AthleteRegistrationResponse(
        Long id,
        Long teamRegistrationId,
        Long athleteId,
        String athleteName,
        String jerseyNumber,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime registrationDate,
        RegistrationStatus status,
        String notes,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime canceledAt
) {
}
