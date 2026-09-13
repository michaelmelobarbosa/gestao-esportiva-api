package br.gov.quixada.esporte.equipe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipeUpdateRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String nome,
        @NotBlank(message = "Responsável é obrigatório")
        @Size(max = 100, message = "Responsável deve ter no máximo 100 caracteres")
        String responsavel
) {
}
