package br.gov.quixada.sports.club.exception;

public class InactiveClubException extends RuntimeException {
    public InactiveClubException(String message) {
        super(message);
    }
}
