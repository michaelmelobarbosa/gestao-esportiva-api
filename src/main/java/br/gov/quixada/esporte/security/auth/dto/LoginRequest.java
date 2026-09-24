package br.gov.quixada.esporte.security.auth.dto;

public record LoginRequest(
        String username,
        String password
) {
}
