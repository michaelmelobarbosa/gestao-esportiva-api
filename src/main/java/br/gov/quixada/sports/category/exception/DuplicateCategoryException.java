package br.gov.quixada.sports.category.exception;

public class DuplicateCategoryException extends RuntimeException {

    public DuplicateCategoryException(String message) {
        super(message);
    }
}
