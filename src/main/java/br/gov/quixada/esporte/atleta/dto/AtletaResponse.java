package br.gov.quixada.esporte.atleta.dto;

import br.gov.quixada.esporte.extras.EnderecoResponse;
import br.gov.quixada.esporte.extras.Sexo;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AtletaResponse(
        Long id,
        String nomeCompleto,
        String cpf,
        LocalDate dataNascimento,
        Integer idade, //CALCULAR NO MAPPER
        EnderecoResponse endereco,
        String telefone,
        Sexo sexo,
        boolean ativo,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataCadastro
) {
}
