package br.gov.quixada.esporte.partida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.gov.quixada.esporte.categoria.Categoria;
import br.gov.quixada.esporte.competicao.Competicao;
import br.gov.quixada.esporte.equipe.Equipe;
import br.gov.quixada.esporte.inscricao.InscricaoEquipe;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClassificacaoService")
class ClassificacaoServiceTest {

    @Mock
    private PartidaRepository repository;

    private ClassificacaoService service;

    @BeforeEach
    void setUp() {
        service = new ClassificacaoService(repository);
    }

    private InscricaoEquipe inscricao(Long id, String equipeNome) {
        var competicao = Competicao.builder().id(1L).nome("Copa").build();
        var categoria = Categoria.builder().id(10L).nome("Sub-20").competicao(competicao).build();
        var equipe = Equipe.builder().id(id).nome(equipeNome).build();
        return InscricaoEquipe.builder()
                .id(id)
                .competicao(competicao)
                .categoria(categoria)
                .equipe(equipe)
                .build();
    }

    private Partida partida(Long id, InscricaoEquipe mandante, InscricaoEquipe visitante,
                            int golsMandante, int golsVisitante, StatusPartida status) {
        return Partida.builder()
                .id(id)
                .inscricaoMandante(mandante)
                .inscricaoVisitante(visitante)
                .golsMandante(golsMandante)
                .golsVisitante(golsVisitante)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("deve calcular pontos, saldo e ordenar por pontos/saldo")
    void deveCalcularClassificacao() {
        var a = inscricao(1L, "Equipe A");
        var b = inscricao(2L, "Equipe B");
        var c = inscricao(3L, "Equipe C");

        when(repository.findByCompeticaoAndCategoria(1L, 10L)).thenReturn(List.of(
                partida(1L, a, b, 2, 1, StatusPartida.FINALIZADA),
                partida(2L, c, a, 1, 0, StatusPartida.FINALIZADA),
                partida(3L, b, c, 0, 0, StatusPartida.CANCELADA)
        ));

        var classificacao = service.calcular(1L, 10L);

        assertThat(classificacao).hasSize(3);
        assertThat(classificacao.get(0).equipeNome()).isEqualTo("Equipe C");
        assertThat(classificacao.get(0).pontos()).isEqualTo(3);
        assertThat(classificacao.get(0).saldoGols()).isEqualTo(1);
        assertThat(classificacao.get(1).equipeNome()).isEqualTo("Equipe A");
        assertThat(classificacao.get(1).pontos()).isEqualTo(3);
        assertThat(classificacao.get(1).saldoGols()).isZero();
        assertThat(classificacao.get(2).equipeNome()).isEqualTo("Equipe B");
        assertThat(classificacao.get(2).pontos()).isZero();
    }

    @Test
    @DisplayName("deve contabilizar empate e aproveitamento")
    void deveContabilizarEmpate() {
        var a = inscricao(1L, "Equipe A");
        var b = inscricao(2L, "Equipe B");

        when(repository.findByCompeticaoAndCategoria(1L, 10L)).thenReturn(List.of(
                partida(1L, a, b, 1, 1, StatusPartida.FINALIZADA)
        ));

        var classificacao = service.calcular(1L, 10L);

        assertThat(classificacao).allSatisfy(linha -> {
            assertThat(linha.pontos()).isEqualTo(1);
            assertThat(linha.empates()).isEqualTo(1);
            assertThat(linha.jogos()).isEqualTo(1);
            assertThat(linha.aproveitamento()).isEqualTo(33.33);
        });
    }

    @Test
    @DisplayName("deve ignorar partidas não finalizadas")
    void deveIgnorarPartidasNaoFinalizadas() {
        var a = inscricao(1L, "Equipe A");
        var b = inscricao(2L, "Equipe B");

        when(repository.findByCompeticaoAndCategoria(1L, 10L)).thenReturn(List.of(
                partida(1L, a, b, 2, 1, StatusPartida.EM_ANDAMENTO),
                partida(2L, a, b, 2, 1, StatusPartida.AGENDADA)
        ));

        assertThat(service.calcular(1L, 10L)).isEmpty();
    }
}
