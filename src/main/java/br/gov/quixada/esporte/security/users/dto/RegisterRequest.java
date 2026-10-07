package br.gov.quixada.esporte.security.users.dto;

public record RegisterRequest(
        String username,
        String password
) {
}
