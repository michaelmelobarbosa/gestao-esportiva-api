package br.gov.quixada.esporte.categoria.dto;

public record CategoriaResponse(
        Long id,
        String nome,
        Integer idadeMinima,
        Integer idadeMaxima,
        Long competicaoId,
        String competicaoNome
) {
}
