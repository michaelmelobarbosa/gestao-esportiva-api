package br.gov.quixada.esporte.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CpfJaCadastradoException extends ResponseStatusException {
    public CpfJaCadastradoException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
