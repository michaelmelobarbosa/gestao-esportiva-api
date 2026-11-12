package br.gov.quixada.sports.registration.exception;

public class TeamRegistrationWithoutAthletesException extends RuntimeException {

    public TeamRegistrationWithoutAthletesException(String message) {
        super(message);
    }
}
