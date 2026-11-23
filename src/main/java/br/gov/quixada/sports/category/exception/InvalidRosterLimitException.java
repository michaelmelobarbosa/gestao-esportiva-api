package br.gov.quixada.sports.category.exception;

public class InvalidRosterLimitException extends RuntimeException {
    public InvalidRosterLimitException(String message) {
        super(message);
    }
}
