package br.gov.quixada.esporte.partida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.gov.quixada.esporte.categoria.Categoria;
import br.gov.quixada.esporte.categoria.CategoriaRepository;
import br.gov.quixada.esporte.competicao.Competicao;
import br.gov.quixada.esporte.competicao.CompeticaoRepository;
import br.gov.quixada.esporte.equipe.Equipe;
import br.gov.quixada.esporte.inscricao.InscricaoEquipe;
import br.gov.quixada.esporte.inscricao.InscricaoEquipeRepository;
import br.gov.quixada.esporte.inscricao.StatusInscricao;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClassificacaoService")
class ClassificacaoServiceTest {

    @Mock
    private PartidaRepository repository;

    @Mock
    private CompeticaoRepository competicaoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private InscricaoEquipeRepository inscricaoEquipeRepository;

    private ClassificacaoService service;

    @BeforeEach
    void setUp() {
        service = new ClassificacaoService(repository, competicaoRepository, categoriaRepository,
                inscricaoEquipeRepository);
        var competicao = Competicao.builder().id(1L).nome("Copa").build();
        var categoria = Categoria.builder().id(10L).nome("Sub-20").competicao(competicao).build();
        lenient().when(competicaoRepository.findById(1L)).thenReturn(Optional.of(competicao));
        lenient().when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
        lenient().when(inscricaoEquipeRepository.findByCompeticaoIdAndCategoriaIdAndStatus(
                1L, 10L, StatusInscricao.APROVADA)).thenReturn(List.of());
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

    @Test
    @DisplayName("deve incluir equipe aprovada mesmo sem partidas")
    void deveIncluirEquipeSemPartidas() {
        var aprovada = inscricao(1L, "Equipe A");
        when(inscricaoEquipeRepository.findByCompeticaoIdAndCategoriaIdAndStatus(
                1L, 10L, StatusInscricao.APROVADA)).thenReturn(List.of(aprovada));
        when(repository.findByCompeticaoAndCategoria(1L, 10L)).thenReturn(List.of());

        var classificacao = service.calcular(1L, 10L);

        assertThat(classificacao).hasSize(1);
        assertThat(classificacao.get(0).equipeNome()).isEqualTo("Equipe A");
        assertThat(classificacao.get(0).pontos()).isZero();
        assertThat(classificacao.get(0).jogos()).isZero();
    }

    @Test
    @DisplayName("deve lançar 404 quando competição não existir")
    void deveLancarNotFoundQuandoCompeticaoNaoExistir() {
        when(competicaoRepository.findById(99L)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.calcular(99L, 10L))
                .isInstanceOf(br.gov.quixada.esporte.competicao.exception.CompeticaoNotFoundException.class);
    }

    @Test
    @DisplayName("deve lançar 404 quando categoria não existir")
    void deveLancarNotFoundQuandoCategoriaNaoExistir() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.calcular(1L, 99L))
                .isInstanceOf(br.gov.quixada.esporte.categoria.exception.CategoriaNotFoundException.class);
    }

    @Test
    @DisplayName("deve rejeitar categoria de outra competição")
    void deveRejeitarCategoriaDeOutraCompeticao() {
        var outra = Competicao.builder().id(2L).nome("Outra").build();
        var categoriaOutra = Categoria.builder().id(10L).nome("Sub-20").competicao(outra).build();
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoriaOutra));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.calcular(1L, 10L))
                .isInstanceOf(br.gov.quixada.esporte.inscricao.exception.InscricaoInvalidaException.class);
    }
}
