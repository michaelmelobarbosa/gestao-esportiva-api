package br.gov.quixada.sports.registration.exception;

public class AthleteAlreadyRegisteredException extends RuntimeException {
    public AthleteAlreadyRegisteredException(String message) {
        super(message);
    }
}
