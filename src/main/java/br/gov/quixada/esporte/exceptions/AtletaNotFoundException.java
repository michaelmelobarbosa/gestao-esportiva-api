package br.gov.quixada.esporte.exceptions;


import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public class AtletaNotFoundException extends ResponseStatusException {
    public AtletaNotFoundException(HttpStatusCode status, @Nullable String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
