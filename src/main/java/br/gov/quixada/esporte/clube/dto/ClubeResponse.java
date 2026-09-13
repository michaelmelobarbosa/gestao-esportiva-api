package br.gov.quixada.esporte.clube.dto;

import br.gov.quixada.esporte.clube.StatusClube;
import br.gov.quixada.esporte.extras.EnderecoResponse;

public record ClubeResponse(
        Long id,
        String nome,
        String responsavel,
        String telefone,
        EnderecoResponse endereco,
        StatusClube status
) {
}
