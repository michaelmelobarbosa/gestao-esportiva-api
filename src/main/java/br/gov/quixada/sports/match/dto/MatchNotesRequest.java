package br.gov.quixada.sports.match.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body for the free-form note endpoint. The note is optional and independent of
 * status: sending {@code null}, omitting the field, or sending blank text clears the value.
 */
public record MatchNotesRequest(
        @Size(max = 500, message = "Notes must have at most 500 characters")
        String notes
) {
}
