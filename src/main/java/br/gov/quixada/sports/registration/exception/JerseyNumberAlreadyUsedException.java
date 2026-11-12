package br.gov.quixada.sports.registration.exception;

public class JerseyNumberAlreadyUsedException extends RuntimeException {
    public JerseyNumberAlreadyUsedException(String message) {
        super(message);
    }
}
