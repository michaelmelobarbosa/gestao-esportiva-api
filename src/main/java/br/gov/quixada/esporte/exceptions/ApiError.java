package br.gov.quixada.esporte.exceptions;

import java.time.LocalDateTime;

public record ApiError(int status, String message, LocalDateTime timestamp) {
     
}
