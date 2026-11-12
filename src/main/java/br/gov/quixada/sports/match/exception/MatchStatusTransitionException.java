package br.gov.quixada.sports.match.exception;

public class MatchStatusTransitionException extends RuntimeException {

    public MatchStatusTransitionException(String message) {
        super(message);
    }
}
