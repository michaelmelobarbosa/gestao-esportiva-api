package br.gov.quixada.esporte.security.users.dto;

import br.gov.quixada.esporte.security.users.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank
        @Size(max = 20)
        String username,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @NotNull(message = "Role é obrigatória")
        UserRole role
) {
}
