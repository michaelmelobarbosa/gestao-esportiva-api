package br.gov.quixada.sports.registration.exception;

public class TeamRegistrationNotFoundException extends RuntimeException {
    public TeamRegistrationNotFoundException(String message) {
        super(message);
    }
}
