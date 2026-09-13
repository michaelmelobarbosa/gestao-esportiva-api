package br.gov.quixada.esporte.clube.dto;

import br.gov.quixada.esporte.clube.StatusClube;

public record ClubeResumoResponse(
        Long id,
        String nome,
        String responsavel,
        String telefone,
        StatusClube status
) {
}
