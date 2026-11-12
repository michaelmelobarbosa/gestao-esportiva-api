package br.gov.quixada.sports.competition.exception;

public class CompetitionStatusTransitionException extends RuntimeException {

    public CompetitionStatusTransitionException(String message) {
        super(message);
    }
}
