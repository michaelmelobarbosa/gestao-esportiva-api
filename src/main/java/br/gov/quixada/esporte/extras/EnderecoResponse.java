package br.gov.quixada.esporte.extras;

public record EnderecoResponse(
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String cep
) {
}
