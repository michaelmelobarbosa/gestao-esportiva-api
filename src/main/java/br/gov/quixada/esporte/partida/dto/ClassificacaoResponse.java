package br.gov.quixada.esporte.partida.dto;

public record ClassificacaoResponse(
        Integer posicao,
        Long inscricaoEquipeId,
        Long equipeId,
        String equipeNome,
        Integer pontos,
        Integer jogos,
        Integer vitorias,
        Integer empates,
        Integer derrotas,
        Integer golsPro,
        Integer golsContra,
        Integer saldoGols,
        Double aproveitamento
) {
}
