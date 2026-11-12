package br.gov.quixada.sports.club.dto;

import br.gov.quixada.sports.shared.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClubCreateRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must have at most 100 characters")
        String name,
        @NotBlank(message = "Manager is required")
        @Size(max = 100, message = "Manager must have at most 100 characters")
        String manager,
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "\\d{10,12}", message = "Phone must contain only digits and be 10 to 12 digits long")
        String phone,
        @NotNull(message = "Address is required")
        @Valid
        AddressRequest address
) {
}
