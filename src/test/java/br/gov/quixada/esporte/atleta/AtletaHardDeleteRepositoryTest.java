package br.gov.quixada.esporte.atleta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import br.gov.quixada.esporte.categoria.Categoria;
import br.gov.quixada.esporte.clube.Clube;
import br.gov.quixada.esporte.competicao.Competicao;
import br.gov.quixada.esporte.competicao.StatusCompeticao;
import br.gov.quixada.esporte.equipe.Equipe;
import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.extras.Sexo;
import br.gov.quixada.esporte.inscricao.InscricaoAtleta;
import br.gov.quixada.esporte.inscricao.InscricaoAtletaRepository;
import br.gov.quixada.esporte.inscricao.InscricaoEquipe;
import br.gov.quixada.esporte.inscricao.StatusInscricao;
import br.gov.quixada.esporte.modalidade.Modalidade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@DisplayName("Atleta — exclusão física e integridade referencial")
class AtletaHardDeleteRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AtletaRepository atletaRepository;

    @Autowired
    private InscricaoAtletaRepository inscricaoAtletaRepository;

    private Atleta umAtleta(String nome) {
        return Atleta.builder()
                .nomeCompleto(nome)
                .cpf("52998224725")
                .dataNascimento(LocalDate.of(2000, 1, 1))
                .endereco(new Endereco("Rua A", "10", "Centro", "Quixadá", "63900-000"))
                .telefone("88999999999")
                .sexo(Sexo.MASCULINO)
                .status(StatusAtleta.INATIVO)
                .build();
    }

    private InscricaoAtleta umaInscricao(Atleta atleta, StatusInscricao status) {
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

        return entityManager.persistFlushFind(InscricaoAtleta.builder()
                .atleta(atleta)
                .inscricaoEquipe(inscricaoEquipe)
                .numeroCamisa("10")
                .status(status)
                .build());
    }

    @Test
    @DisplayName("não deve permitir apagar atleta enquanto existir inscrição vinculada")
    void naoDeveApagarAtletaComInscricao() {
        Atleta atleta = entityManager.persistFlushFind(umAtleta("João Silva"));
        umaInscricao(atleta, StatusInscricao.CANCELADA);
        entityManager.clear();

        Atleta recarregado = atletaRepository.findById(atleta.getId()).orElseThrow();
        atletaRepository.delete(recarregado);

        assertThatThrownBy(() -> atletaRepository.flush())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("deleteByAtletaIdAndStatusIn deve remover as inscrições que liberam vaga")
    void deveRemoverInscricoesQueLiberamVaga() {
        Atleta atleta = entityManager.persistFlushFind(umAtleta("Maria Souza"));
        umaInscricao(atleta, StatusInscricao.CANCELADA);
        umaInscricao(atleta, StatusInscricao.REJEITADA);

        long removidas = inscricaoAtletaRepository.deleteByAtletaIdAndStatusIn(
                atleta.getId(), StatusInscricao.LIBERAM_VAGA);
        entityManager.flush();
        atletaRepository.delete(atleta);
        atletaRepository.flush();

        assertThat(removidas).isEqualTo(2);
        assertThat(atletaRepository.findById(atleta.getId())).isEmpty();
    }
}
