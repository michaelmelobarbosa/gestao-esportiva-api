package br.gov.quixada.sports.registration.exception;

public class RosterLimitExceededException extends RuntimeException {

    public RosterLimitExceededException(String message) {
        super(message);
    }
}
