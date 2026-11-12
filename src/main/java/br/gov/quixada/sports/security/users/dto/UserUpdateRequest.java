package br.gov.quixada.sports.security.users.dto;

import br.gov.quixada.sports.security.users.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank
        @Size(max = 20)
        String username,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @Size(min = 8, max = 72)
        String password,

        @NotNull(message = "Role is required")
        UserRole role
) {
}
