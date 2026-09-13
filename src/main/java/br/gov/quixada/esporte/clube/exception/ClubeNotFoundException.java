package br.gov.quixada.esporte.clube.exception;

public class ClubeNotFoundException extends RuntimeException {
    public ClubeNotFoundException(String message) {
        super(message);
    }
}
