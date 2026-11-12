package br.gov.quixada.sports.athlete.exception;

public class AthleteNotFoundException extends RuntimeException {
    public AthleteNotFoundException(String message) {
        super(message);
    }
}
