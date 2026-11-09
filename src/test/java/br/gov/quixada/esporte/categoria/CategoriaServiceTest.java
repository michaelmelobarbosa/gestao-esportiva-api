package br.gov.quixada.esporte.categoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import br.gov.quixada.esporte.categoria.exception.CategoriaInativaException;
import br.gov.quixada.esporte.categoria.exception.CategoriaNotFoundException;
import br.gov.quixada.esporte.categoria.exception.IdadeInvalidaException;
import br.gov.quixada.esporte.categoria.exception.CategoriaDuplicadaException;
import br.gov.quixada.esporte.common.error.VinculoAtivoException;
import br.gov.quixada.esporte.inscricao.StatusInscricao;
import br.gov.quixada.esporte.competicao.Competicao;
import br.gov.quixada.esporte.competicao.StatusCompeticao;
import br.gov.quixada.esporte.competicao.CompeticaoRepository;
import br.gov.quixada.esporte.competicao.exception.CompeticaoNaoEditavelException;
import br.gov.quixada.esporte.competicao.exception.CompeticaoNotFoundException;
import br.gov.quixada.esporte.inscricao.InscricaoEquipeRepository;
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
@DisplayName("CategoriaService")
class CategoriaServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private CategoriaRepository repository;

    @Mock
    private CompeticaoRepository competicaoRepository;

    @Mock
    private InscricaoEquipeRepository inscricaoEquipeRepository;

    private CategoriaService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Fortaleza"));
        service = new CategoriaService(repository, competicaoRepository, inscricaoEquipeRepository, clock);
    }

    private Categoria umaCategoria(StatusCategoria status) {
        return Categoria.builder()
                .nome("Sub-15")
                .idadeMinima(13)
                .idadeMaxima(15)
                .status(status)
                .build();
    }

    private Competicao umaCompeticao() {
        return Competicao.builder()
                .nome("Copa Teste")
                .ano(2026)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("deve buscar todos quando o nome for nulo")
        void deveBuscarTodosQuandoNomeNulo() {
            Page<Categoria> esperado = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(esperado);

            Page<Categoria> resultado = service.findAll(null, null, PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNomeContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("deve buscar todos quando o nome for em branco")
        void deveBuscarTodosQuandoNomeEmBranco() {
            Page<Categoria> esperado = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(esperado);

            Page<Categoria> resultado = service.findAll("   ", null, PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNomeContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("deve filtrar por nome quando informado")
        void deveFiltrarPorNomeQuandoInformado() {
            Page<Categoria> esperado = Page.empty();
            when(repository.findByNomeContainingIgnoreCase("sub", PAGEABLE)).thenReturn(esperado);

            Page<Categoria> resultado = service.findAll("sub", null, PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findByNomeContainingIgnoreCase("sub", PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("deve retornar a categoria quando existir")
        void deveRetornarCategoriaQuandoExistir() {
            Categoria categoria = umaCategoria(StatusCategoria.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(categoria));

            Categoria resultado = service.findByIdOrThrowNotFound(1L);

            assertThat(resultado).isSameAs(categoria);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("deve lançar CategoriaNotFoundException quando não existir")
        void deveLancarNotFoundQuandoNaoExistir() {
            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(CategoriaNotFoundException.class)
                    .hasMessage("Categoria não encontrada");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("deve validar idades, forçar ATIVO, definir competição e salvar")
        void deveSalvarComCompeticaoEStatusAtivo() {
            Categoria categoria = umaCategoria(StatusCategoria.INATIVO);
            Competicao competicao = umaCompeticao();
            when(competicaoRepository.findById(1L)).thenReturn(Optional.of(competicao));
            when(repository.save(any(Categoria.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

            Categoria salva = service.save(categoria, 1L);

            assertThat(salva).isSameAs(categoria);
            assertThat(salva.getStatus()).isEqualTo(StatusCategoria.ATIVO);
            assertThat(salva.getCompeticao()).isSameAs(competicao);
            verify(repository).save(categoria);
        }

        @Test
        @DisplayName("deve lançar IdadeInvalidaException quando mínima for maior que a máxima")
        void deveLancarIdadeInvalidaQuandoMinimaMaiorQueMaxima() {
            Categoria categoria = Categoria.builder()
                    .nome("Sub-15")
                    .idadeMinima(15)
                    .idadeMaxima(13)
                    .build();

            assertThatThrownBy(() -> service.save(categoria, 1L))
                    .isInstanceOf(IdadeInvalidaException.class)
                    .hasMessage("Idade mínima não pode ser maior que a idade máxima");
        }

        @Test
        @DisplayName("deve lançar CompeticaoNotFoundException quando a competição não existir")
        void deveLancarNotFoundQuandoCompeticaoNaoExistir() {
            Categoria categoria = umaCategoria(StatusCategoria.ATIVO);

            assertThatThrownBy(() -> service.save(categoria, 99L))
                    .isInstanceOf(CompeticaoNotFoundException.class)
                    .hasMessage("Competição não encontrada");
        }
    }

    @Nested
    @DisplayName("ativar / inativar")
    class AtivarInativar {

        @Test
        @DisplayName("deve ativar quando estiver INATIVO")
        void deveAtivarQuandoInativo() {
            Categoria categoria = umaCategoria(StatusCategoria.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(categoria));

            Categoria resultado = service.ativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusCategoria.ATIVO);
            verify(repository, never()).save(any(Categoria.class));
        }

        @Test
        @DisplayName("deve manter ATIVO quando já estiver ATIVO (idempotência)")
        void deveManterAtivoQuandoJaAtivo() {
            Categoria categoria = umaCategoria(StatusCategoria.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(categoria));

            Categoria resultado = service.ativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusCategoria.ATIVO);
            verify(repository, never()).save(any(Categoria.class));
        }

        @Test
        @DisplayName("deve lançar CategoriaNotFoundException quando ativar id inexistente")
        void deveLancarNotFoundQuandoAtivarInexistente() {
            assertThatThrownBy(() -> service.ativar(1L))
                    .isInstanceOf(CategoriaNotFoundException.class)
                    .hasMessage("Categoria não encontrada");
        }

        @Test
        @DisplayName("deve inativar quando estiver ATIVO")
        void deveInativarQuandoAtivo() {
            Categoria categoria = umaCategoria(StatusCategoria.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(categoria));

            Categoria resultado = service.inativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusCategoria.INATIVO);
            verify(repository, never()).save(any(Categoria.class));
        }

        @Test
        @DisplayName("deve manter INATIVO quando já estiver INATIVO (idempotência)")
        void deveManterInativoQuandoJaInativo() {
            Categoria categoria = umaCategoria(StatusCategoria.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(categoria));

            Categoria resultado = service.inativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusCategoria.INATIVO);
            verify(repository, never()).save(any(Categoria.class));
        }

        @Test
        @DisplayName("deve lançar CategoriaNotFoundException quando inativar id inexistente")
        void deveLancarNotFoundQuandoInativarInexistente() {
            assertThatThrownBy(() -> service.inativar(1L))
                    .isInstanceOf(CategoriaNotFoundException.class)
                    .hasMessage("Categoria não encontrada");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        private Categoria novosDados() {
            return Categoria.builder()
                    .nome("Sub-17")
                    .idadeMinima(15)
                    .idadeMaxima(17)
                    .build();
        }

        @Test
        @DisplayName("deve copiar nome e idades e não chamar save")
        void deveAtualizarDadosQuandoAtivo() {
            Categoria existente = umaCategoria(StatusCategoria.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            Categoria resultado = service.update(1L, novosDados());

            assertThat(resultado).isSameAs(existente);
            assertThat(resultado.getNome()).isEqualTo("Sub-17");
            assertThat(resultado.getIdadeMinima()).isEqualTo(15);
            assertThat(resultado.getIdadeMaxima()).isEqualTo(17);
            verify(repository, never()).save(any(Categoria.class));
        }

        @Test
        @DisplayName("deve lançar CategoriaInativaException quando a categoria estiver INATIVA")
        void deveLancarInativaQuandoAtualizarInativa() {
            Categoria existente = umaCategoria(StatusCategoria.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.update(1L, novosDados()))
                    .isInstanceOf(CategoriaInativaException.class)
                    .hasMessage("Categoria está inativa, reative antes de editar");
        }

        @Test
        @DisplayName("deve lançar IdadeInvalidaException quando as idades forem inválidas")
        void deveLancarIdadeInvalidaQuandoIdadesInvalidas() {
            Categoria existente = umaCategoria(StatusCategoria.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));
            Categoria novos = Categoria.builder()
                    .nome("Sub-17")
                    .idadeMinima(17)
                    .idadeMaxima(15)
                    .build();

            assertThatThrownBy(() -> service.update(1L, novos))
                    .isInstanceOf(IdadeInvalidaException.class)
                    .hasMessage("Idade mínima não pode ser maior que a idade máxima");
        }

        @Test
        @DisplayName("deve lançar CategoriaNotFoundException quando o id não existir")
        void deveLancarNotFoundQuandoAtualizarInexistente() {
            assertThatThrownBy(() -> service.update(1L, novosDados()))
                    .isInstanceOf(CategoriaNotFoundException.class)
                    .hasMessage("Categoria não encontrada");
        }
    }

    @Nested
    @DisplayName("regras de negócio")
    class RegrasDeNegocio {

        private Competicao competicaoComStatus(StatusCompeticao status) {
            return Competicao.builder().id(1L).nome("Copa").ano(2026).status(status).build();
        }

        @Test
        @DisplayName("deve bloquear faixa duplicada ao criar")
        void deveBloquearFaixaDuplicadaAoCriar() {
            when(competicaoRepository.findById(1L)).thenReturn(Optional.of(competicaoComStatus(StatusCompeticao.INSCRICOES_ABERTAS)));
            when(repository.existsByCompeticaoIdAndIdadeMinimaAndIdadeMaximaAndStatus(
                    1L, 13, 15, StatusCategoria.ATIVO)).thenReturn(true);

            assertThatThrownBy(() -> service.save(umaCategoria(StatusCategoria.ATIVO), 1L))
                    .isInstanceOf(CategoriaDuplicadaException.class);
        }

        @Test
        @DisplayName("deve bloquear faixa duplicada ao atualizar")
        void deveBloquearFaixaDuplicadaAoAtualizar() {
            Categoria existente = Categoria.builder().id(2L).nome("Sub-15").idadeMinima(13).idadeMaxima(15)
                    .status(StatusCategoria.ATIVO).competicao(competicaoComStatus(StatusCompeticao.INSCRICOES_ABERTAS)).build();
            when(repository.findById(2L)).thenReturn(Optional.of(existente));
            when(repository.existsByCompeticaoIdAndIdadeMinimaAndIdadeMaximaAndStatusAndIdNot(
                    1L, 13, 15, StatusCategoria.ATIVO, 2L)).thenReturn(true);

            assertThatThrownBy(() -> service.update(2L, umaCategoria(StatusCategoria.ATIVO)))
                    .isInstanceOf(CategoriaDuplicadaException.class);
        }

        @Test
        @DisplayName("deve bloquear faixa duplicada ao ativar")
        void deveBloquearFaixaDuplicadaAoAtivar() {
            Categoria existente = Categoria.builder().id(2L).nome("Sub-15").idadeMinima(13).idadeMaxima(15)
                    .status(StatusCategoria.INATIVO).competicao(competicaoComStatus(StatusCompeticao.INSCRICOES_ABERTAS)).build();
            when(repository.findById(2L)).thenReturn(Optional.of(existente));
            when(repository.existsByCompeticaoIdAndIdadeMinimaAndIdadeMaximaAndStatusAndIdNot(
                    1L, 13, 15, StatusCategoria.ATIVO, 2L)).thenReturn(true);

            assertThatThrownBy(() -> service.ativar(2L))
                    .isInstanceOf(CategoriaDuplicadaException.class);
        }

        @Test
        @DisplayName("não deve inativar categoria com inscrição ativa")
        void naoDeveInativarComInscricaoAtiva() {
            Categoria existente = Categoria.builder().id(2L).status(StatusCategoria.ATIVO).build();
            when(repository.findById(2L)).thenReturn(Optional.of(existente));
            when(inscricaoEquipeRepository.existsByCategoriaIdAndStatusIn(2L, StatusInscricao.OCUPAM_VAGA))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.inativar(2L))
                    .isInstanceOf(VinculoAtivoException.class);
        }

        @Test
        @DisplayName("deve bloquear criação em competição finalizada")
        void deveBloquearCriacaoEmCompeticaoFinalizada() {
            when(competicaoRepository.findById(1L)).thenReturn(Optional.of(competicaoComStatus(StatusCompeticao.FINALIZADA)));

            assertThatThrownBy(() -> service.save(umaCategoria(StatusCategoria.ATIVO), 1L))
                    .isInstanceOf(CompeticaoNaoEditavelException.class);
        }
    }
}
