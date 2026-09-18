package br.gov.quixada.esporte.equipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import br.gov.quixada.esporte.clube.Clube;
import br.gov.quixada.esporte.clube.ClubeRepository;
import br.gov.quixada.esporte.clube.exception.ClubeNotFoundException;
import br.gov.quixada.esporte.equipe.exception.EquipeInativaException;
import br.gov.quixada.esporte.equipe.exception.EquipeNotFoundException;
import br.gov.quixada.esporte.modalidade.Modalidade;
import br.gov.quixada.esporte.modalidade.ModalidadeRepository;
import br.gov.quixada.esporte.modalidade.exception.ModalidadeNotFoundException;
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
@DisplayName("EquipeService")
class EquipeServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private EquipeRepository repository;

    @Mock
    private ClubeRepository clubeRepository;

    @Mock
    private ModalidadeRepository modalidadeRepository;

    private EquipeService service;

    @BeforeEach
    void setUp() {
        service = new EquipeService(repository, clubeRepository, modalidadeRepository);
    }

    private Equipe umaEquipe(StatusEquipe status) {
        return Equipe.builder()
                .nome("Equipe A")
                .responsavel("Fulano")
                .status(status)
                .build();
    }

    private Clube umClube() {
        return Clube.builder()
                .nome("Clube A")
                .build();
    }

    private Modalidade umaModalidade() {
        return Modalidade.builder()
                .nome("Futebol")
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("deve buscar todos quando o nome for nulo")
        void deveBuscarTodosQuandoNomeNulo() {
            Page<Equipe> esperado = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(esperado);

            Page<Equipe> resultado = service.findAll(null, PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNomeContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("deve buscar todos quando o nome for em branco")
        void deveBuscarTodosQuandoNomeEmBranco() {
            Page<Equipe> esperado = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(esperado);

            Page<Equipe> resultado = service.findAll("   ", PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNomeContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("deve filtrar por nome quando informado")
        void deveFiltrarPorNomeQuandoInformado() {
            Page<Equipe> esperado = Page.empty();
            when(repository.findByNomeContainingIgnoreCase("equipe", PAGEABLE)).thenReturn(esperado);

            Page<Equipe> resultado = service.findAll("equipe", PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findByNomeContainingIgnoreCase("equipe", PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("deve retornar a equipe quando existir")
        void deveRetornarEquipeQuandoExistir() {
            Equipe equipe = umaEquipe(StatusEquipe.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(equipe));

            Equipe resultado = service.findByIdOrThrowNotFound(1L);

            assertThat(resultado).isSameAs(equipe);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("deve lançar EquipeNotFoundException quando não existir")
        void deveLancarNotFoundQuandoNaoExistir() {
            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(EquipeNotFoundException.class)
                    .hasMessage("Equipe não encontrada");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("deve ativar, definir clube e modalidade e salvar")
        void deveSalvarComClubeEModalidade() {
            Equipe equipe = umaEquipe(StatusEquipe.INATIVO);
            Clube clube = umClube();
            Modalidade modalidade = umaModalidade();
            when(clubeRepository.findById(1L)).thenReturn(Optional.of(clube));
            when(modalidadeRepository.findById(2L)).thenReturn(Optional.of(modalidade));
            when(repository.save(any(Equipe.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

            Equipe salva = service.save(equipe, 1L, 2L);

            assertThat(salva).isSameAs(equipe);
            assertThat(salva.getStatus()).isEqualTo(StatusEquipe.ATIVO);
            assertThat(salva.getClube()).isSameAs(clube);
            assertThat(salva.getModalidade()).isSameAs(modalidade);
            verify(repository).save(equipe);
        }

        @Test
        @DisplayName("deve lançar ClubeNotFoundException quando o clube não existir")
        void deveLancarNotFoundQuandoClubeNaoExistir() {
            Equipe equipe = umaEquipe(StatusEquipe.ATIVO);

            assertThatThrownBy(() -> service.save(equipe, 99L, 2L))
                    .isInstanceOf(ClubeNotFoundException.class)
                    .hasMessage("Clube não encontrado");
        }

        @Test
        @DisplayName("deve lançar ModalidadeNotFoundException quando a modalidade não existir")
        void deveLancarNotFoundQuandoModalidadeNaoExistir() {
            Equipe equipe = umaEquipe(StatusEquipe.ATIVO);
            when(clubeRepository.findById(1L)).thenReturn(Optional.of(umClube()));

            assertThatThrownBy(() -> service.save(equipe, 1L, 99L))
                    .isInstanceOf(ModalidadeNotFoundException.class)
                    .hasMessage("Modalidade não encontrada");
        }
    }

    @Nested
    @DisplayName("ativar / inativar")
    class AtivarInativar {

        @Test
        @DisplayName("deve ativar quando estiver INATIVO")
        void deveAtivarQuandoInativo() {
            Equipe equipe = umaEquipe(StatusEquipe.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(equipe));

            Equipe resultado = service.ativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusEquipe.ATIVO);
            verify(repository, never()).save(any(Equipe.class));
        }

        @Test
        @DisplayName("deve manter ATIVO quando já estiver ATIVO (idempotência)")
        void deveManterAtivoQuandoJaAtivo() {
            Equipe equipe = umaEquipe(StatusEquipe.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(equipe));

            Equipe resultado = service.ativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusEquipe.ATIVO);
            verify(repository, never()).save(any(Equipe.class));
        }

        @Test
        @DisplayName("deve lançar EquipeNotFoundException quando ativar id inexistente")
        void deveLancarNotFoundQuandoAtivarInexistente() {
            assertThatThrownBy(() -> service.ativar(1L))
                    .isInstanceOf(EquipeNotFoundException.class)
                    .hasMessage("Equipe não encontrada");
        }

        @Test
        @DisplayName("deve inativar quando estiver ATIVO")
        void deveInativarQuandoAtivo() {
            Equipe equipe = umaEquipe(StatusEquipe.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(equipe));

            Equipe resultado = service.inativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusEquipe.INATIVO);
            verify(repository, never()).save(any(Equipe.class));
        }

        @Test
        @DisplayName("deve manter INATIVO quando já estiver INATIVO (idempotência)")
        void deveManterInativoQuandoJaInativo() {
            Equipe equipe = umaEquipe(StatusEquipe.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(equipe));

            Equipe resultado = service.inativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusEquipe.INATIVO);
            verify(repository, never()).save(any(Equipe.class));
        }

        @Test
        @DisplayName("deve lançar EquipeNotFoundException quando inativar id inexistente")
        void deveLancarNotFoundQuandoInativarInexistente() {
            assertThatThrownBy(() -> service.inativar(1L))
                    .isInstanceOf(EquipeNotFoundException.class)
                    .hasMessage("Equipe não encontrada");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        private Equipe novosDados() {
            return Equipe.builder()
                    .nome("Equipe B")
                    .responsavel("Beltrano")
                    .build();
        }

        @Test
        @DisplayName("deve copiar nome e responsável e não chamar save")
        void deveAtualizarDadosQuandoAtivo() {
            Equipe existente = umaEquipe(StatusEquipe.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            Equipe resultado = service.update(1L, novosDados());

            assertThat(resultado).isSameAs(existente);
            assertThat(resultado.getNome()).isEqualTo("Equipe B");
            assertThat(resultado.getResponsavel()).isEqualTo("Beltrano");
            verify(repository, never()).save(any(Equipe.class));
        }

        @Test
        @DisplayName("deve lançar EquipeInativaException quando a equipe estiver INATIVA")
        void deveLancarInativaQuandoAtualizarInativa() {
            Equipe existente = umaEquipe(StatusEquipe.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.update(1L, novosDados()))
                    .isInstanceOf(EquipeInativaException.class)
                    .hasMessage("Equipe está inativa, reative antes de editar");
        }

        @Test
        @DisplayName("deve lançar EquipeNotFoundException quando o id não existir")
        void deveLancarNotFoundQuandoAtualizarInexistente() {
            assertThatThrownBy(() -> service.update(1L, novosDados()))
                    .isInstanceOf(EquipeNotFoundException.class)
                    .hasMessage("Equipe não encontrada");
        }
    }
}
