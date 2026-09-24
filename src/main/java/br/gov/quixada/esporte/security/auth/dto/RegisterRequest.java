package br.gov.quixada.esporte.security.auth.dto;

import br.gov.quixada.esporte.security.users.UserRole;

public record RegisterRequest(
        String username,
        String password,
        UserRole role
) {
}
