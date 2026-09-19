package br.gov.quixada.esporte.users.dto;

public record AuthenticationRequest(
        String userName,
        String password
) {
}
