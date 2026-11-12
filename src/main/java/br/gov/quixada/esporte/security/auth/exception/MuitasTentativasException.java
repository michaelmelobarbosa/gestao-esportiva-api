package br.gov.quixada.esporte.security.auth.exception;

public class MuitasTentativasException extends RuntimeException {

    private final long retryAfterSeconds;

    public MuitasTentativasException(long retryAfterSeconds) {
        super("Muitas tentativas de login");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
