package br.gov.quixada.sports.shared;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "Street is required")
        @Size(max = 150)
        String street,
        @NotBlank(message = "Enter a number or N/A")
        @Size(max = 10)
        String number,
        @NotBlank(message = "Neighborhood is required")
        @Size(max=50)
        String neighborhood,
        @NotBlank(message = "City is required")
        @Size(max=50)
        String city,
        @NotBlank(message = "Postal code is required")
        @Pattern(regexp="\\d{5}-\\d{3}", message = "Postal code must follow the pattern 00000-000")
        String postalCode
) {
}
