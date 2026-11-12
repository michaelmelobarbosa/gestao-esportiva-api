package br.gov.quixada.sports.registration.exception;

public class AthleteRegistrationNotFoundException extends RuntimeException {
    public AthleteRegistrationNotFoundException(String message) {
        super(message);
    }
}
