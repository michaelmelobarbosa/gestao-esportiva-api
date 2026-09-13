package br.gov.quixada.esporte.exceptions;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String MSG_VALIDACAO = "Erro de validação";
    private static final String MSG_JSON_INVALIDO = "JSON inválido";
    private static final String MSG_CONFLITO_DADOS = "Conflito de dados";

    @ExceptionHandler(AtletaNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(AtletaNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CpfJaCadastradoException.class)
    public ResponseEntity<ApiError> handleCpfCadastrado(CpfJaCadastradoException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(AtletaInativoException.class)
    public ResponseEntity<ApiError> handleInativo(AtletaInativoException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CategoriaNotFoundException.class)
    public ResponseEntity<ApiError> handleCategoriaNotFound(CategoriaNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CompeticaoNotFoundException.class)
    public ResponseEntity<ApiError> handleCompeticaoNotFound(CompeticaoNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(IdadeInvalidaException.class)
    public ResponseEntity<ApiError> handleIdadeInvalida(IdadeInvalidaException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.FieldErrors> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> new ApiError.FieldErrors(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();

        return build(HttpStatus.BAD_REQUEST, MSG_VALIDACAO, request, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldErrors> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiError.FieldErrors(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        return build(HttpStatus.BAD_REQUEST, MSG_VALIDACAO, request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, MSG_JSON_INVALIDO, request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, MSG_CONFLITO_DADOS, request, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           HttpServletRequest request, List<ApiError.FieldErrors> errors) {
        ApiError body = new ApiError(status.value(), message, LocalDateTime.now(), request.getRequestURI(), errors);
        return ResponseEntity.status(status).body(body);
    }
}
