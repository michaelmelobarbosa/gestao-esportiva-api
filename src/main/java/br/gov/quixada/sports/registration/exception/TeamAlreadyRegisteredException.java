package br.gov.quixada.sports.registration.exception;

public class TeamAlreadyRegisteredException extends RuntimeException {

    public TeamAlreadyRegisteredException(String message) {
        super(message);
    }
}
