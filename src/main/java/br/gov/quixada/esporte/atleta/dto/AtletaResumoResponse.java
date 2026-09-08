package br.gov.quixada.esporte.atleta.dto;

import br.gov.quixada.esporte.extras.Sexo;
import br.gov.quixada.esporte.extras.StatusAtleta;

public record AtletaResumoResponse(
        Long id,
        String nomeCompleto,
        String cpf,
        Integer idade,
        String telefone,
        Sexo sexo,
        StatusAtleta status
) {
}
