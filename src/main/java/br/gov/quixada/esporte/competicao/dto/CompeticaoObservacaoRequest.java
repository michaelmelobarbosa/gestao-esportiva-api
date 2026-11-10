package br.gov.quixada.esporte.competicao.dto;

import jakarta.validation.constraints.Size;

/**
 * Corpo do endpoint de anotação livre. A observação é opcional e independe do
 * status: enviar {@code null}, omitir o campo ou mandar texto em branco limpa o valor.
 */
public record CompeticaoObservacaoRequest(
        @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
        String observacao
) {
}
