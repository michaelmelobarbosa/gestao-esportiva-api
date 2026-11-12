package br.gov.quixada.sports.modality.exception;

public class ModalityNotFoundException extends RuntimeException {
    public ModalityNotFoundException(String message) {
        super(message);
    }
}
