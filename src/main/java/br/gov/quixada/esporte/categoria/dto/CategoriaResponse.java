package br.gov.quixada.esporte.categoria.dto;

import br.gov.quixada.esporte.categoria.StatusCategoria;

public record CategoriaResponse(
        Long id,
        String nome,
        Integer idadeMinima,
        Integer idadeMaxima,
        Long competicaoId,
        String competicaoNome,
        StatusCategoria status
) {
}
