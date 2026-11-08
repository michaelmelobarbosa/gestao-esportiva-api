package br.gov.quixada.esporte.partida;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PartidaRepository repository;

    @Test
    @DisplayName("deve salvar partida e buscar por competição/categoria")
    void deveBuscarPorCompeticaoECategoria() {
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
                .dataHora(LocalDateTime.of(2026, 4, 1, 15, 0))
                .local("Ginásio")
                .inscricaoMandante(inscricaoEquipe)
                .inscricaoVisitante(inscricaoEquipe)
                .status(StatusPartida.AGENDADA)
                .build());

        var pagina = repository.findByCompeticaoAndCategoria(competicao.getId(), categoria.getId(),
                PageRequest.of(0, 10));

        assertThat(pagina.getContent()).extracting(Partida::getId).containsExactly(partida.getId());
    }
}
