package br.gov.quixada.esporte.exceptions;

public class CompeticaoNotFoundException extends RuntimeException {
    public CompeticaoNotFoundException(String message) {
        super(message);
    }
}
