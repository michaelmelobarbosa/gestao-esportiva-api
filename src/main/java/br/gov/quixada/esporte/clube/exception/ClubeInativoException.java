package br.gov.quixada.esporte.clube.exception;

public class ClubeInativoException extends RuntimeException {
    public ClubeInativoException(String message) {
        super(message);
    }
}
