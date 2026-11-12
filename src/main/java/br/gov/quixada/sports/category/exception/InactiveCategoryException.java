package br.gov.quixada.sports.category.exception;

public class InactiveCategoryException extends RuntimeException {
    public InactiveCategoryException(String message) {
        super(message);
    }
}
