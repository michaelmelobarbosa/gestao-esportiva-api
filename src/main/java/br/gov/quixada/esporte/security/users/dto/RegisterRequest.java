package br.gov.quixada.esporte.security.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(max = 20)
        String username,

        @NotBlank
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(max = 20)
        String password
) {
}
