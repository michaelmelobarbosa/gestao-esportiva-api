package br.gov.quixada.sports.security.users.dto;

import br.gov.quixada.sports.security.users.UserRole;
import br.gov.quixada.sports.security.users.UserStatus;

public record UserResponse(
        Long id,
        String username,
        String email,
        UserRole role,
        UserStatus status
) {
}
