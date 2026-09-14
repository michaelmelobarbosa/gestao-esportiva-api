package br.gov.quixada.esporte.inscricao.dto;

import br.gov.quixada.esporte.inscricao.StatusInscricao;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record InscricaoEquipeResponse(
        Long id,
        Long competicaoId,
        String competicaoNome,
        Long categoriaId,
        String categoriaNome,
        Long equipeId,
        String equipeNome,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataInscricao,
        StatusInscricao status
) {
}
