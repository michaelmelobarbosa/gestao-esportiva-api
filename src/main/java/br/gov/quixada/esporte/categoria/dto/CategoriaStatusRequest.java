package br.gov.quixada.esporte.categoria.dto;

import br.gov.quixada.esporte.categoria.StatusCategoria;
import jakarta.validation.constraints.NotNull;

public record CategoriaStatusRequest(
        @NotNull(message = "Status é obrigatório")
        StatusCategoria status
) {
}
