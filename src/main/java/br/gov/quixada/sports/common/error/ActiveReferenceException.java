package br.gov.quixada.sports.common.error;

public class ActiveReferenceException extends RuntimeException {

    public ActiveReferenceException(String message) {
        super(message);
    }
}
