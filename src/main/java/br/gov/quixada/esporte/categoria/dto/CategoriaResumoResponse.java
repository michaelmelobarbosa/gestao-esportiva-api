package br.gov.quixada.esporte.categoria.dto;

import br.gov.quixada.esporte.categoria.StatusCategoria;

public record CategoriaResumoResponse(
        Long id,
        String nome,
        Integer idadeMinima,
        Integer idadeMaxima,
        Long competicaoId,
        StatusCategoria status
) {
}
