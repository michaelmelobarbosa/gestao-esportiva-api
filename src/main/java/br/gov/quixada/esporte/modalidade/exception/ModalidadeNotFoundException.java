package br.gov.quixada.esporte.modalidade.exception;

public class ModalidadeNotFoundException extends RuntimeException {
    public ModalidadeNotFoundException(String message) {
        super(message);
    }
}
