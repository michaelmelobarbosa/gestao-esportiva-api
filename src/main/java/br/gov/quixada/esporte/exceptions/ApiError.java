package br.gov.quixada.esporte.exceptions;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(int status, String message, LocalDateTime timestamp, String path, List<ApiError.FieldErrors> errors) {
    public record FieldErrors(String field, String message) {
    }
}
