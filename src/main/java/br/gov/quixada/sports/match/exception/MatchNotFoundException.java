package br.gov.quixada.sports.match.exception;

public class MatchNotFoundException extends RuntimeException {

    public MatchNotFoundException(String message) {
        super(message);
    }
}
