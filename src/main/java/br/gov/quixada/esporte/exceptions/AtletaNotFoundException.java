package br.gov.quixada.esporte.exceptions;

public class AtletaNotFoundException extends RuntimeException {
    public AtletaNotFoundException(String message) {
        super(message);
    }
}
