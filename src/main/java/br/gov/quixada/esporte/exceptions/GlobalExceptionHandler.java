package br.gov.quixada.esporte.exceptions;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AtletaNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(AtletaNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(CpfJaCadastradoException.class)
    public ResponseEntity<ApiError> handleConflict(CpfJaCadastradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(HttpStatus.CONFLICT.value(), ex.getMessage(), LocalDateTime.now()));
    }

}
