package br.gov.quixada.sports.athlete.dto;

import br.gov.quixada.sports.shared.AddressRequest;
import br.gov.quixada.sports.shared.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;


public record AthleteCreateRequest(
        @NotBlank(message = "Full name is required")
        @Size(max=150, message = "Full name must have at most 150 characters")
        String fullName,
        @NotBlank(message = "CPF is required")
        @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{11}", message = "CPF must be in the format 000.000.000-00 or 00000000000")
        @CPF(message = "Invalid CPF")
        String cpf,
        @Past(message = "Birth date must be in the past")
        @NotNull(message = "Birth date is required")
        LocalDate birthDate,
        @NotNull(message = "Address is required")
        @Valid
        AddressRequest address,
        @NotNull(message = "Gender is required")
        Gender gender,
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "\\d{10,12}", message = "Phone must contain only digits and have 10 to 12 characters")
        String phone
) {}
