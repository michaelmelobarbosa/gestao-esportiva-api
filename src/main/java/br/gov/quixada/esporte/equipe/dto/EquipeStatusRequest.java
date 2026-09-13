package br.gov.quixada.esporte.equipe.dto;

import br.gov.quixada.esporte.equipe.StatusEquipe;
import jakarta.validation.constraints.NotNull;

public record EquipeStatusRequest(
        @NotNull(message = "Status é obrigatório")
        StatusEquipe status
) {
}
