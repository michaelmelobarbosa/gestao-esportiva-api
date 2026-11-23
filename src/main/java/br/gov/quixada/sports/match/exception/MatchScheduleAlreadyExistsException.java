package br.gov.quixada.sports.match.exception;

public class MatchScheduleAlreadyExistsException extends RuntimeException {

    public MatchScheduleAlreadyExistsException(String message) {
        super(message);
    }
}
