package br.gov.quixada.sports.athlete.exception;

public class InactiveAthleteException extends RuntimeException {
    public InactiveAthleteException(String message) {
        super(message);
    }
}
