package br.gov.quixada.esporte.inscricao.dto;

import br.gov.quixada.esporte.inscricao.StatusInscricao;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record InscricaoAtletaResponse(
        Long id,
        Long inscricaoEquipeId,
        Long atletaId,
        String atletaNome,
        String numeroCamisa,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataInscricao,
        StatusInscricao status
) {
}
