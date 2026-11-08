package br.gov.quixada.esporte.partida.exception;

public class PartidaNotFoundException extends RuntimeException {

    public PartidaNotFoundException(String message) {
        super(message);
    }
}
