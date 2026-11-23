package br.gov.quixada.sports.security.users.exception;

public class InvalidAuditQueryException extends RuntimeException {
    public InvalidAuditQueryException(String message) {
        super(message);
    }
}
