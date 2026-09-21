package br.gov.quixada.esporte.users.dto;

import br.gov.quixada.esporte.users.UserRoles;

public record RegisterRequest(
        String username,
        String password,
        UserRoles role
) {
}
