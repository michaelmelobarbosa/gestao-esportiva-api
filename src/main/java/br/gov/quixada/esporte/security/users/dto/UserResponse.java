package br.gov.quixada.esporte.security.users.dto;

import br.gov.quixada.esporte.security.users.UserRole;
import br.gov.quixada.esporte.security.users.UserStatus;

public record UserResponse(
        Long id,
        String username,
        String email,
        UserRole role,
        UserStatus status
) {
}
