package br.gov.quixada.esporte.partida.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record PartidaCreateRequest(
        @NotNull(message = "Inscrição mandante é obrigatória")
        Long inscricaoMandanteId,
        @NotNull(message = "Inscrição visitante é obrigatória")
        Long inscricaoVisitanteId,
        @NotNull(message = "Rodada é obrigatória")
        @Positive(message = "Rodada deve ser positiva")
        Integer rodada,
        @NotNull(message = "Data e hora são obrigatórias")
        LocalDateTime dataHora,
        @Size(max = 150, message = "Local deve ter no máximo 150 caracteres")
        String local,
        @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
        String observacao
) {
}
