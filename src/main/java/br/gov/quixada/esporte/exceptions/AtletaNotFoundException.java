package br.gov.quixada.esporte.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AtletaNotFoundException extends ResponseStatusException {
    public AtletaNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
