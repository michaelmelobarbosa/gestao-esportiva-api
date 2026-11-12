package br.gov.quixada.sports.registration.exception;

public class AthleteOutsideCategoryException extends RuntimeException {
    public AthleteOutsideCategoryException(String message) {
        super(message);
    }
}
