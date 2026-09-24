package br.gov.quixada.esporte.security.users.dto;

public record LoginRequest(
        String username,
        String password
) {
}
