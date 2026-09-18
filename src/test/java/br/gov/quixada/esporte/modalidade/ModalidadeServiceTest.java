package br.gov.quixada.esporte.modalidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import br.gov.quixada.esporte.modalidade.exception.ModalidadeInativaException;
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
@DisplayName("ModalidadeService")
class ModalidadeServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private ModalidadeRepository repository;

    private ModalidadeService service;

    @BeforeEach
    void setUp() {
        service = new ModalidadeService(repository);
    }

    private Modalidade umaModalidade(StatusModalidade status) {
        return Modalidade.builder()
                .nome("Futebol")
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("deve buscar todos quando o nome for nulo")
        void deveBuscarTodosQuandoNomeNulo() {
            Page<Modalidade> esperado = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(esperado);

            Page<Modalidade> resultado = service.findAll(null, PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNomeContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("deve buscar todos quando o nome for em branco")
        void deveBuscarTodosQuandoNomeEmBranco() {
            Page<Modalidade> esperado = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(esperado);

            Page<Modalidade> resultado = service.findAll("   ", PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNomeContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("deve filtrar por nome quando informado")
        void deveFiltrarPorNomeQuandoInformado() {
            Page<Modalidade> esperado = Page.empty();
            when(repository.findByNomeContainingIgnoreCase("futebol", PAGEABLE)).thenReturn(esperado);

            Page<Modalidade> resultado = service.findAll("futebol", PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findByNomeContainingIgnoreCase("futebol", PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("deve retornar a modalidade quando existir")
        void deveRetornarModalidadeQuandoExistir() {
            Modalidade modalidade = umaModalidade(StatusModalidade.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(modalidade));

            Modalidade resultado = service.findByIdOrThrowNotFound(1L);

            assertThat(resultado).isSameAs(modalidade);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("deve lançar ModalidadeNotFoundException quando não existir")
        void deveLancarNotFoundQuandoNaoExistir() {
            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(ModalidadeNotFoundException.class)
                    .hasMessage("Modalidade não encontrada");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("deve forçar status ATIVO e salvar")
        void deveForcarStatusAtivoESalvar() {
            Modalidade modalidade = umaModalidade(StatusModalidade.INATIVO);
            when(repository.save(any(Modalidade.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

            Modalidade salva = service.save(modalidade);

            assertThat(salva).isSameAs(modalidade);
            assertThat(salva.getStatus()).isEqualTo(StatusModalidade.ATIVO);
            verify(repository).save(modalidade);
        }
    }

    @Nested
    @DisplayName("ativar / inativar")
    class AtivarInativar {

        @Test
        @DisplayName("deve ativar quando estiver INATIVO")
        void deveAtivarQuandoInativo() {
            Modalidade modalidade = umaModalidade(StatusModalidade.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(modalidade));

            Modalidade resultado = service.ativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusModalidade.ATIVO);
            verify(repository, never()).save(any(Modalidade.class));
        }

        @Test
        @DisplayName("deve manter ATIVO quando já estiver ATIVO (idempotência)")
        void deveManterAtivoQuandoJaAtivo() {
            Modalidade modalidade = umaModalidade(StatusModalidade.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(modalidade));

            Modalidade resultado = service.ativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusModalidade.ATIVO);
            verify(repository, never()).save(any(Modalidade.class));
        }

        @Test
        @DisplayName("deve lançar ModalidadeNotFoundException quando ativar id inexistente")
        void deveLancarNotFoundQuandoAtivarInexistente() {
            assertThatThrownBy(() -> service.ativar(1L))
                    .isInstanceOf(ModalidadeNotFoundException.class)
                    .hasMessage("Modalidade não encontrada");
        }

        @Test
        @DisplayName("deve inativar quando estiver ATIVO")
        void deveInativarQuandoAtivo() {
            Modalidade modalidade = umaModalidade(StatusModalidade.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(modalidade));

            Modalidade resultado = service.inativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusModalidade.INATIVO);
            verify(repository, never()).save(any(Modalidade.class));
        }

        @Test
        @DisplayName("deve manter INATIVO quando já estiver INATIVO (idempotência)")
        void deveManterInativoQuandoJaInativo() {
            Modalidade modalidade = umaModalidade(StatusModalidade.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(modalidade));

            Modalidade resultado = service.inativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusModalidade.INATIVO);
            verify(repository, never()).save(any(Modalidade.class));
        }

        @Test
        @DisplayName("deve lançar ModalidadeNotFoundException quando inativar id inexistente")
        void deveLancarNotFoundQuandoInativarInexistente() {
            assertThatThrownBy(() -> service.inativar(1L))
                    .isInstanceOf(ModalidadeNotFoundException.class)
                    .hasMessage("Modalidade não encontrada");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("deve copiar o nome e não chamar save")
        void deveAtualizarNomeQuandoAtivo() {
            Modalidade existente = umaModalidade(StatusModalidade.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            Modalidade resultado = service.update(1L, Modalidade.builder().nome("Basquete").build());

            assertThat(resultado).isSameAs(existente);
            assertThat(resultado.getNome()).isEqualTo("Basquete");
            verify(repository, never()).save(any(Modalidade.class));
        }

        @Test
        @DisplayName("deve lançar ModalidadeInativaException quando a modalidade estiver INATIVA")
        void deveLancarInativaQuandoAtualizarInativa() {
            Modalidade existente = umaModalidade(StatusModalidade.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.update(1L, Modalidade.builder().nome("Basquete").build()))
                    .isInstanceOf(ModalidadeInativaException.class)
                    .hasMessage("Modalidade está inativa, reative antes de editar");
        }

        @Test
        @DisplayName("deve lançar ModalidadeNotFoundException quando o id não existir")
        void deveLancarNotFoundQuandoAtualizarInexistente() {
            assertThatThrownBy(() -> service.update(1L, Modalidade.builder().nome("Basquete").build()))
                    .isInstanceOf(ModalidadeNotFoundException.class)
                    .hasMessage("Modalidade não encontrada");
        }
    }
}
