package br.gov.quixada.sports.club.exception;

public class ClubNotFoundException extends RuntimeException {
    public ClubNotFoundException(String message) {
        super(message);
    }
}
