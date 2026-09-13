package br.gov.quixada.esporte.categoria.dto;

public record CategoriaResumoResponse(
        Long id,
        String nome,
        Integer idadeMinima,
        Integer idadeMaxima,
        Long competicaoId
) {
}
