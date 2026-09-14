package br.gov.quixada.esporte.inscricao.dto;

import br.gov.quixada.esporte.inscricao.StatusInscricao;
import jakarta.validation.constraints.NotNull;

public record InscricaoEquipeStatusRequest(
        @NotNull(message = "Status é obrigatório")
        StatusInscricao status
) {
}
