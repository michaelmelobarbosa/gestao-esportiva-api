package br.gov.quixada.esporte.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.Nullable;

public class CpfJaCadastradoException extends ResponseStatusException {
    
    public CpfJaCadastradoException(HttpStatus status, @Nullable String message){
        super(HttpStatus.CONFLICT, message);
    }
}
