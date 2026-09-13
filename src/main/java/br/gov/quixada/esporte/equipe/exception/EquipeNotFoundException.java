package br.gov.quixada.esporte.equipe.exception;

public class EquipeNotFoundException extends RuntimeException {
    public EquipeNotFoundException(String message) {
        super(message);
    }
}
