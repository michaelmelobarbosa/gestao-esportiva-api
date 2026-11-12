package br.gov.quixada.sports.team.exception;

public class InactiveTeamException extends RuntimeException {
    public InactiveTeamException(String message) {
        super(message);
    }
}
