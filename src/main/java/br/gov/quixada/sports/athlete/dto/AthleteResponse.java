package br.gov.quixada.sports.athlete.dto;

import br.gov.quixada.sports.shared.AddressResponse;
import br.gov.quixada.sports.shared.Gender;
import br.gov.quixada.sports.athlete.AthleteStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AthleteResponse(
        Long id,
        String fullName,
        String cpf,
        LocalDate birthDate,
        Integer age,
        AddressResponse address,
        String phone,
        Gender gender,
        AthleteStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
}
