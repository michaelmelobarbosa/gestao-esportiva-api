package br.gov.quixada.esporte.atleta.exception;

public class AtletaNotFoundException extends RuntimeException {
    public AtletaNotFoundException(String message) {
        super(message);
    }
}
