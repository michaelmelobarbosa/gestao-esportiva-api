package br.gov.quixada.esporte.competicao.exception;

public class CompeticaoNotFoundException extends RuntimeException {
    public CompeticaoNotFoundException(String message) {
        super(message);
    }
}
