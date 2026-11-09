package br.gov.quixada.esporte.inscricao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import br.gov.quixada.esporte.categoria.Categoria;
import br.gov.quixada.esporte.categoria.StatusCategoria;
import br.gov.quixada.esporte.categoria.CategoriaRepository;
import br.gov.quixada.esporte.categoria.exception.CategoriaNotFoundException;
import br.gov.quixada.esporte.competicao.Competicao;
import br.gov.quixada.esporte.competicao.CompeticaoRepository;
import br.gov.quixada.esporte.competicao.StatusCompeticao;
import br.gov.quixada.esporte.competicao.exception.CompeticaoNotFoundException;
import br.gov.quixada.esporte.equipe.Equipe;
import br.gov.quixada.esporte.equipe.EquipeRepository;
import br.gov.quixada.esporte.equipe.StatusEquipe;
import br.gov.quixada.esporte.equipe.exception.EquipeInativaException;
import br.gov.quixada.esporte.equipe.exception.EquipeNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.EquipeJaInscritaException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoEquipeNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoEquipeSemAtletasException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoInvalidaException;
import br.gov.quixada.esporte.inscricao.exception.InscricoesEncerradasException;
import br.gov.quixada.esporte.inscricao.exception.TransicaoStatusInvalidaException;
import br.gov.quixada.esporte.modalidade.Modalidade;
import br.gov.quixada.esporte.partida.PartidaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("InscricaoEquipeService")
class InscricaoEquipeServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private InscricaoEquipeRepository repository;

    @Mock
    private CompeticaoRepository competicaoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private EquipeRepository equipeRepository;

    @Mock
    private InscricaoAtletaRepository inscricaoAtletaRepository;

    @Mock
    private PartidaService partidaService;

    private InscricaoEquipeService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Fortaleza"));
        service = new InscricaoEquipeService(repository, inscricaoAtletaRepository, competicaoRepository,
                categoriaRepository, equipeRepository, partidaService, clock);
    }

    private Modalidade umaModalidade(Long id) {
        return Modalidade.builder().id(id).nome("Futebol").build();
    }

    private Competicao umaCompeticao(Long id, Long modalidadeId, StatusCompeticao status) {
        return Competicao.builder()
                .id(id)
                .nome("Copa Teste")
                .ano(2026)
                .modalidade(umaModalidade(modalidadeId))
                .status(status)
                .build();
    }

    private Categoria umaCategoria(Long id, Competicao competicao) {
        return Categoria.builder()
                .id(id)
                .nome("Sub-15")
                .idadeMinima(13)
                .idadeMaxima(15)
                .competicao(competicao)
                .status(br.gov.quixada.esporte.categoria.StatusCategoria.ATIVO)
                .build();
    }

    private Equipe umaEquipe(Long id, Long modalidadeId, StatusEquipe status) {
        return Equipe.builder()
                .id(id)
                .nome("Equipe A")
                .modalidade(umaModalidade(modalidadeId))
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("deve buscar todas quando o id da competição for nulo")
        void deveBuscarTodasQuandoCompeticaoNula() {
            Page<InscricaoEquipe> esperado = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(esperado);

            Page<InscricaoEquipe> resultado = service.findAll(null, null, PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
        }

        @Test
        @DisplayName("deve filtrar por competição quando informado")
        void deveFiltrarPorCompeticaoQuandoInformado() {
            Page<InscricaoEquipe> esperado = Page.empty();
            when(repository.findByCompeticaoId(1L, PAGEABLE)).thenReturn(esperado);

            Page<InscricaoEquipe> resultado = service.findAll(1L, null, PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findByCompeticaoId(1L, PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("deve retornar a inscrição quando existir")
        void deveRetornarInscricaoQuandoExistir() {
            InscricaoEquipe inscricao = InscricaoEquipe.builder().build();
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));

            InscricaoEquipe resultado = service.findByIdOrThrowNotFound(1L);

            assertThat(resultado).isSameAs(inscricao);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("deve lançar InscricaoEquipeNotFoundException quando não existir")
        void deveLancarNotFoundQuandoNaoExistir() {
            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(InscricaoEquipeNotFoundException.class)
                    .hasMessage("Inscrição de equipe não encontrada");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("deve definir competição, categoria, equipe e status PENDENTE e salvar")
        void deveSalvarQuandoDadosValidos() {
            Competicao competicao = umaCompeticao(1L, 10L, StatusCompeticao.INSCRICOES_ABERTAS);
            Categoria categoria = umaCategoria(2L, competicao);
            Equipe equipe = umaEquipe(3L, 10L, StatusEquipe.ATIVO);
            when(competicaoRepository.findById(1L)).thenReturn(Optional.of(competicao));
            when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria));
            when(equipeRepository.findById(3L)).thenReturn(Optional.of(equipe));
            when(repository.save(any(InscricaoEquipe.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

            InscricaoEquipe inscricao = InscricaoEquipe.builder().build();
            InscricaoEquipe salva = service.save(inscricao, 1L, 2L, 3L);

            assertThat(salva).isSameAs(inscricao);
            assertThat(salva.getCompeticao()).isSameAs(competicao);
            assertThat(salva.getCategoria()).isSameAs(categoria);
            assertThat(salva.getEquipe()).isSameAs(equipe);
            assertThat(salva.getStatus()).isEqualTo(StatusInscricao.PENDENTE);
            verify(repository).save(inscricao);
        }

        @Test
        @DisplayName("deve lançar CompeticaoNotFoundException quando a competição não existir")
        void deveLancarNotFoundQuandoCompeticaoNaoExistir() {
            InscricaoEquipe inscricao = InscricaoEquipe.builder().build();

            assertThatThrownBy(() -> service.save(inscricao, 99L, 2L, 3L))
                    .isInstanceOf(CompeticaoNotFoundException.class)
                    .hasMessage("Competição não encontrada");
        }

        @Test
        @DisplayName("deve lançar CategoriaNotFoundException quando a categoria não existir")
        void deveLancarNotFoundQuandoCategoriaNaoExistir() {
            Competicao competicao = umaCompeticao(1L, 10L, StatusCompeticao.INSCRICOES_ABERTAS);
            when(competicaoRepository.findById(1L)).thenReturn(Optional.of(competicao));
            InscricaoEquipe inscricao = InscricaoEquipe.builder().build();

            assertThatThrownBy(() -> service.save(inscricao, 1L, 99L, 3L))
                    .isInstanceOf(CategoriaNotFoundException.class)
                    .hasMessage("Categoria não encontrada");
        }

        @Test
        @DisplayName("deve lançar EquipeNotFoundException quando a equipe não existir")
        void deveLancarNotFoundQuandoEquipeNaoExistir() {
            Competicao competicao = umaCompeticao(1L, 10L, StatusCompeticao.INSCRICOES_ABERTAS);
            Categoria categoria = umaCategoria(2L, competicao);
            when(competicaoRepository.findById(1L)).thenReturn(Optional.of(competicao));
            when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria));
            InscricaoEquipe inscricao = InscricaoEquipe.builder().build();

            assertThatThrownBy(() -> service.save(inscricao, 1L, 2L, 99L))
                    .isInstanceOf(EquipeNotFoundException.class)
                    .hasMessage("Equipe não encontrada");
        }

        @Test
        @DisplayName("deve lançar InscricoesEncerradasException quando a competição não estiver com inscrições abertas")
        void deveLancarInscricoesEncerradas() {
            Competicao competicao = umaCompeticao(1L, 10L, StatusCompeticao.PLANEJAMENTO);
            Categoria categoria = umaCategoria(2L, competicao);
            Equipe equipe = umaEquipe(3L, 10L, StatusEquipe.ATIVO);
            when(competicaoRepository.findById(1L)).thenReturn(Optional.of(competicao));
            when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria));
            when(equipeRepository.findById(3L)).thenReturn(Optional.of(equipe));
            InscricaoEquipe inscricao = InscricaoEquipe.builder().build();

            assertThatThrownBy(() -> service.save(inscricao, 1L, 2L, 3L))
                    .isInstanceOf(InscricoesEncerradasException.class)
                    .hasMessage("Competição não está com inscrições abertas");
        }

        @Test
        @DisplayName("deve lançar EquipeInativaException quando a equipe estiver INATIVA")
        void deveLancarEquipeInativa() {
            Competicao competicao = umaCompeticao(1L, 10L, StatusCompeticao.INSCRICOES_ABERTAS);
            Categoria categoria = umaCategoria(2L, competicao);
            Equipe equipe = umaEquipe(3L, 10L, StatusEquipe.INATIVO);
            when(competicaoRepository.findById(1L)).thenReturn(Optional.of(competicao));
            when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria));
            when(equipeRepository.findById(3L)).thenReturn(Optional.of(equipe));
            InscricaoEquipe inscricao = InscricaoEquipe.builder().build();

            assertThatThrownBy(() -> service.save(inscricao, 1L, 2L, 3L))
                    .isInstanceOf(EquipeInativaException.class)
                    .hasMessage("Equipe está inativa, não pode ser inscrita");
        }

        @Test
        @DisplayName("deve lançar InscricaoInvalidaException quando a categoria for de outra competição")
        void deveLancarInvalidaQuandoCategoriaDeOutraCompeticao() {
            Competicao competicao = umaCompeticao(1L, 10L, StatusCompeticao.INSCRICOES_ABERTAS);
            Categoria categoria = umaCategoria(2L, umaCompeticao(99L, 10L, StatusCompeticao.INSCRICOES_ABERTAS));
            Equipe equipe = umaEquipe(3L, 10L, StatusEquipe.ATIVO);
            when(competicaoRepository.findById(1L)).thenReturn(Optional.of(competicao));
            when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria));
            when(equipeRepository.findById(3L)).thenReturn(Optional.of(equipe));
            InscricaoEquipe inscricao = InscricaoEquipe.builder().build();

            assertThatThrownBy(() -> service.save(inscricao, 1L, 2L, 3L))
                    .isInstanceOf(InscricaoInvalidaException.class)
                    .hasMessage("Categoria não pertence à competição informada");
        }

        @Test
        @DisplayName("deve lançar InscricaoInvalidaException quando a modalidade da equipe divergir")
        void deveLancarInvalidaQuandoModalidadeDivergir() {
            Competicao competicao = umaCompeticao(1L, 10L, StatusCompeticao.INSCRICOES_ABERTAS);
            Categoria categoria = umaCategoria(2L, competicao);
            Equipe equipe = umaEquipe(3L, 11L, StatusEquipe.ATIVO);
            when(competicaoRepository.findById(1L)).thenReturn(Optional.of(competicao));
            when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria));
            when(equipeRepository.findById(3L)).thenReturn(Optional.of(equipe));
            InscricaoEquipe inscricao = InscricaoEquipe.builder().build();

            assertThatThrownBy(() -> service.save(inscricao, 1L, 2L, 3L))
                    .isInstanceOf(InscricaoInvalidaException.class)
                    .hasMessage("Modalidade da equipe difere da modalidade da competição");
        }

        @Test
        @DisplayName("deve bloquear inscrição duplicada da mesma equipe na competição")
        void deveBloquearEquipeDuplicada() {
            Competicao competicao = umaCompeticao(1L, 10L, StatusCompeticao.INSCRICOES_ABERTAS);
            Categoria categoria = umaCategoria(2L, competicao);
            Equipe equipe = umaEquipe(3L, 10L, StatusEquipe.ATIVO);
            when(competicaoRepository.findById(1L)).thenReturn(Optional.of(competicao));
            when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria));
            when(equipeRepository.findById(3L)).thenReturn(Optional.of(equipe));
            when(repository.existsByCompeticaoIdAndEquipeIdAndStatusIn(1L, 3L, StatusInscricao.OCUPAM_VAGA))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.save(InscricaoEquipe.builder().build(), 1L, 2L, 3L))
                    .isInstanceOf(EquipeJaInscritaException.class)
                    .hasMessage("Equipe já possui inscrição ativa nesta competição");
        }
    }

    @Nested
    @DisplayName("alterarStatus")
    class AlterarStatus {

        @Test
        @DisplayName("deve alterar o status e não chamar save")
        void deveAlterarStatus() {
            InscricaoEquipe inscricao = InscricaoEquipe.builder().id(1L).status(StatusInscricao.PENDENTE).build();
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));
            when(inscricaoAtletaRepository.countByInscricaoEquipeIdAndStatusIn(
                    eq(1L), anyCollection())).thenReturn(1L);

            InscricaoEquipe resultado = service.alterarStatus(1L, StatusInscricao.APROVADA);

            assertThat(resultado.getStatus()).isEqualTo(StatusInscricao.APROVADA);
            verify(repository, never()).save(any(InscricaoEquipe.class));
        }

        @Test
        @DisplayName("deve manter o status quando for o mesmo (idempotência)")
        void deveManterStatusQuandoIgual() {
            InscricaoEquipe inscricao = InscricaoEquipe.builder().status(StatusInscricao.APROVADA).build();
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));

            InscricaoEquipe resultado = service.alterarStatus(1L, StatusInscricao.APROVADA);

            assertThat(resultado.getStatus()).isEqualTo(StatusInscricao.APROVADA);
            verify(repository, never()).save(any(InscricaoEquipe.class));
        }

        @Test
        @DisplayName("deve cancelar em cascata os atletas e as partidas")
        void deveCancelarEmCascata() {
            InscricaoEquipe inscricao = InscricaoEquipe.builder().id(1L).status(StatusInscricao.PENDENTE).build();
            InscricaoAtleta atleta = InscricaoAtleta.builder().id(10L).status(StatusInscricao.PENDENTE).build();
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));
            when(inscricaoAtletaRepository.findByInscricaoEquipeId(1L)).thenReturn(java.util.List.of(atleta));

            service.alterarStatus(1L, StatusInscricao.CANCELADA);

            assertThat(atleta.getStatus()).isEqualTo(StatusInscricao.CANCELADA);
            verify(partidaService).cancelarPorInscricao(1L);
        }

        @Test
        @DisplayName("deve inativar em cascata os atletas")
        void deveInativarEmCascata() {
            InscricaoEquipe inscricao = InscricaoEquipe.builder().id(1L).status(StatusInscricao.APROVADA).build();
            InscricaoAtleta atleta = InscricaoAtleta.builder().id(10L).status(StatusInscricao.PENDENTE).build();
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));
            when(inscricaoAtletaRepository.findByInscricaoEquipeId(1L)).thenReturn(java.util.List.of(atleta));

            service.alterarStatus(1L, StatusInscricao.INATIVA);

            assertThat(atleta.getStatus()).isEqualTo(StatusInscricao.INATIVA);
            verify(partidaService).cancelarPorInscricao(1L);
        }

        @Test
        @DisplayName("deve bloquear transição a partir de CANCELADA")
        void deveBloquearTransicaoDeCancelada() {
            InscricaoEquipe inscricao = InscricaoEquipe.builder().id(1L).status(StatusInscricao.CANCELADA).build();
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));

            assertThatThrownBy(() -> service.alterarStatus(1L, StatusInscricao.PENDENTE))
                    .isInstanceOf(TransicaoStatusInvalidaException.class);
        }

        @Test
        @DisplayName("deve lançar InscricaoEquipeNotFoundException quando o id não existir")
        void deveLancarNotFoundQuandoInexistente() {
            assertThatThrownBy(() -> service.alterarStatus(1L, StatusInscricao.APROVADA))
                    .isInstanceOf(InscricaoEquipeNotFoundException.class)
                    .hasMessage("Inscrição de equipe não encontrada");
        }
    }

    @Nested
    @DisplayName("regras de negócio")
    class RegrasDeNegocio {

        private InscricaoEquipe inscricaoCompleta(StatusInscricao status, StatusCompeticao statusCompeticao,
                                                  StatusCategoria statusCategoria, StatusEquipe statusEquipe) {
            Competicao competicao = umaCompeticao(1L, 10L, statusCompeticao);
            Categoria categoria = Categoria.builder()
                    .id(2L)
                    .nome("Sub-15")
                    .idadeMinima(13)
                    .idadeMaxima(15)
                    .competicao(competicao)
                    .status(statusCategoria)
                    .build();
            Equipe equipe = umaEquipe(3L, 10L, statusEquipe);
            return InscricaoEquipe.builder()
                    .id(1L)
                    .status(status)
                    .competicao(competicao)
                    .categoria(categoria)
                    .equipe(equipe)
                    .build();
        }

        @Test
        @DisplayName("deve rejeitar aprovação de equipe sem atletas")
        void deveRejeitarAprovacaoSemAtletas() {
            InscricaoEquipe inscricao = inscricaoCompleta(StatusInscricao.PENDENTE,
                    StatusCompeticao.INSCRICOES_ABERTAS, StatusCategoria.ATIVO, StatusEquipe.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));
            when(inscricaoAtletaRepository.countByInscricaoEquipeIdAndStatusIn(eq(1L), anyCollection()))
                    .thenReturn(0L);

            assertThatThrownBy(() -> service.alterarStatus(1L, StatusInscricao.APROVADA))
                    .isInstanceOf(InscricaoEquipeSemAtletasException.class)
                    .hasMessage("Inscrição de equipe precisa de ao menos um atleta para ser aprovada");
        }

        @Test
        @DisplayName("deve rejeitar reativação com competição encerrada")
        void deveRejeitarReativacaoComCompeticaoEncerrada() {
            InscricaoEquipe inscricao = inscricaoCompleta(StatusInscricao.INATIVA,
                    StatusCompeticao.PLANEJAMENTO, StatusCategoria.ATIVO, StatusEquipe.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));

            assertThatThrownBy(() -> service.alterarStatus(1L, StatusInscricao.PENDENTE))
                    .isInstanceOf(InscricoesEncerradasException.class);
        }

        @Test
        @DisplayName("deve rejeitar reativação com equipe inativa")
        void deveRejeitarReativacaoComEquipeInativa() {
            InscricaoEquipe inscricao = inscricaoCompleta(StatusInscricao.INATIVA,
                    StatusCompeticao.INSCRICOES_ABERTAS, StatusCategoria.ATIVO, StatusEquipe.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));

            assertThatThrownBy(() -> service.alterarStatus(1L, StatusInscricao.PENDENTE))
                    .isInstanceOf(EquipeInativaException.class);
        }

        @Test
        @DisplayName("deve rejeitar reativação com categoria inativa")
        void deveRejeitarReativacaoComCategoriaInativa() {
            InscricaoEquipe inscricao = inscricaoCompleta(StatusInscricao.INATIVA,
                    StatusCompeticao.INSCRICOES_ABERTAS, StatusCategoria.INATIVO, StatusEquipe.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));

            assertThatThrownBy(() -> service.alterarStatus(1L, StatusInscricao.PENDENTE))
                    .isInstanceOf(br.gov.quixada.esporte.categoria.exception.CategoriaInativaException.class);
        }

        @Test
        @DisplayName("deve rejeitar reativação quando já existe outra inscrição ativa da equipe")
        void deveRejeitarReativacaoComDuplicidade() {
            InscricaoEquipe inscricao = inscricaoCompleta(StatusInscricao.INATIVA,
                    StatusCompeticao.INSCRICOES_ABERTAS, StatusCategoria.ATIVO, StatusEquipe.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));
            when(repository.existsByCompeticaoIdAndEquipeIdAndStatusInAndIdNot(
                    1L, 3L, StatusInscricao.OCUPAM_VAGA, 1L)).thenReturn(true);

            assertThatThrownBy(() -> service.alterarStatus(1L, StatusInscricao.PENDENTE))
                    .isInstanceOf(EquipeJaInscritaException.class);
        }

        @Test
        @DisplayName("não deve alterar atleta REJEITADA na cascata INATIVA")
        void naoDeveAlterarRejeitadaNaCascataInativa() {
            InscricaoEquipe inscricao = inscricaoCompleta(StatusInscricao.APROVADA,
                    StatusCompeticao.INSCRICOES_ABERTAS, StatusCategoria.ATIVO, StatusEquipe.ATIVO);
            InscricaoAtleta atleta = InscricaoAtleta.builder()
                    .id(10L).status(StatusInscricao.REJEITADA).build();
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));
            when(inscricaoAtletaRepository.findByInscricaoEquipeId(1L)).thenReturn(List.of(atleta));

            service.alterarStatus(1L, StatusInscricao.INATIVA);

            assertThat(atleta.getStatus()).isEqualTo(StatusInscricao.REJEITADA);
        }

        @Test
        @DisplayName("deve bloquear inscrição em categoria inativa")
        void deveBloquearInscricaoEmCategoriaInativa() {
            Competicao competicao = umaCompeticao(1L, 10L, StatusCompeticao.INSCRICOES_ABERTAS);
            Categoria categoria = Categoria.builder().id(2L).nome("Sub-15").idadeMinima(13).idadeMaxima(15)
                    .competicao(competicao).status(StatusCategoria.INATIVO).build();
            Equipe equipe = umaEquipe(3L, 10L, StatusEquipe.ATIVO);
            when(competicaoRepository.findById(1L)).thenReturn(Optional.of(competicao));
            when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria));
            when(equipeRepository.findById(3L)).thenReturn(Optional.of(equipe));

            assertThatThrownBy(() -> service.save(InscricaoEquipe.builder().build(), 1L, 2L, 3L))
                    .isInstanceOf(br.gov.quixada.esporte.categoria.exception.CategoriaInativaException.class);
        }
    }
}
