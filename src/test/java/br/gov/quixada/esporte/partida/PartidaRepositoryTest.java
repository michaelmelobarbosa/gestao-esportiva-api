package br.gov.quixada.esporte.partida;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import br.gov.quixada.esporte.categoria.Categoria;
import br.gov.quixada.esporte.clube.Clube;
import br.gov.quixada.esporte.competicao.Competicao;
import br.gov.quixada.esporte.competicao.StatusCompeticao;
import br.gov.quixada.esporte.equipe.Equipe;
import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.inscricao.InscricaoEquipe;
import br.gov.quixada.esporte.inscricao.StatusInscricao;
import br.gov.quixada.esporte.modalidade.Modalidade;

@DataJpaTest
@DisplayName("Partida — persistência")
class PartidaRepositoryTest {

    private static final LocalDateTime DATA_HORA = LocalDateTime.of(2026, 4, 1, 15, 0);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PartidaRepository repository;

    private record Cenario(Competicao competicao, Categoria categoria,
                           InscricaoEquipe inscricao, Partida partida) {
    }

    private Cenario cenario() {
        var modalidade = entityManager.persistFlushFind(Modalidade.builder().nome("Futebol").build());

        var competicao = entityManager.persistFlushFind(Competicao.builder()
                .nome("Copa Teste")
                .ano(2026)
                .modalidade(modalidade)
                .dataInicioInscricao(LocalDate.of(2026, 1, 1))
                .dataFimInscricao(LocalDate.of(2026, 2, 1))
                .dataInicio(LocalDate.of(2026, 3, 1))
                .dataFim(LocalDate.of(2026, 12, 31))
                .status(StatusCompeticao.INSCRICOES_ABERTAS)
                .build());

        var categoria = entityManager.persistFlushFind(Categoria.builder()
                .nome("Sub-20")
                .idadeMinima(18)
                .idadeMaxima(20)
                .competicao(competicao)
                .build());

        var clube = entityManager.persistFlushFind(Clube.builder()
                .nome("Clube A")
                .responsavel("Fulano")
                .telefone("88999999999")
                .endereco(new Endereco("Rua A", "10", "Centro", "Quixadá", "63900-000"))
                .build());

        var equipe = entityManager.persistFlushFind(Equipe.builder()
                .nome("Equipe A")
                .clube(clube)
                .modalidade(modalidade)
                .responsavel("Fulano")
                .build());

        var inscricaoEquipe = entityManager.persistFlushFind(InscricaoEquipe.builder()
                .competicao(competicao)
                .categoria(categoria)
                .equipe(equipe)
                .status(StatusInscricao.APROVADA)
                .build());

        var partida = entityManager.persistFlushFind(Partida.builder()
                .rodada(1)
                .dataHora(DATA_HORA)
                .local("Ginásio")
                .inscricaoMandante(inscricaoEquipe)
                .inscricaoVisitante(inscricaoEquipe)
                .status(StatusPartida.AGENDADA)
                .build());

        return new Cenario(competicao, categoria, inscricaoEquipe, partida);
    }

    @Test
    @DisplayName("deve salvar partida e buscar por competição/categoria")
    void deveBuscarPorCompeticaoECategoria() {
        var cenario = cenario();

        var pagina = repository.findByCompeticaoAndCategoria(
                cenario.competicao().getId(), cenario.categoria().getId(), PageRequest.of(0, 10));

        assertThat(pagina.getContent()).extracting(Partida::getId)
                .containsExactly(cenario.partida().getId());
    }

    @Test
    @DisplayName("deve filtrar por competição isoladamente")
    void deveFiltrarPorCompeticao() {
        var cenario = cenario();

        var pagina = repository.search(cenario.competicao().getId(), null, null, PageRequest.of(0, 10));

        assertThat(pagina.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("deve detectar equipe com partida na mesma rodada")
    void deveDetectarEquipeNaRodada() {
        var cenario = cenario();
        var ids = List.of(cenario.inscricao().getId());

        assertThat(repository.existsEquipeNaRodada(ids, 1, -1L, StatusPartida.CANCELADA)).isTrue();
        assertThat(repository.existsEquipeNaRodada(ids, 2, -1L, StatusPartida.CANCELADA)).isFalse();
    }

    @Test
    @DisplayName("deve detectar conflito de data e hora")
    void deveDetectarConflitoDataHora() {
        var cenario = cenario();
        var ids = List.of(cenario.inscricao().getId());

        assertThat(repository.existsConflitoDataHora(ids, DATA_HORA, -1L, StatusPartida.CANCELADA)).isTrue();
        assertThat(repository.existsConflitoDataHora(ids, DATA_HORA.plusHours(1), -1L, StatusPartida.CANCELADA))
                .isFalse();
    }

    @Test
    @DisplayName("deve cancelar partidas em cascata pela competição e pela inscrição")
    void deveCancelarPartidasEmCascata() {
        var cenario = cenario();

        repository.cancelarPorInscricao(cenario.inscricao().getId(),
                List.of(StatusPartida.AGENDADA, StatusPartida.EM_ANDAMENTO), StatusPartida.CANCELADA);
        entityManager.clear();

        assertThat(repository.findById(cenario.partida().getId()).orElseThrow().getStatus())
                .isEqualTo(StatusPartida.CANCELADA);
    }
}
