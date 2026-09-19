package br.gov.quixada.esporte.users.dto;

public record RegisterRequest(
        String username,
        String password,
        Enum role
) {
}
