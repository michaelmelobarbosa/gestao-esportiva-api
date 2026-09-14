package br.gov.quixada.esporte.clube;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import br.gov.quixada.esporte.clube.exception.ClubeInativoException;
import br.gov.quixada.esporte.clube.exception.ClubeNotFoundException;
import br.gov.quixada.esporte.extras.Endereco;
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
@DisplayName("ClubeService")
class ClubeServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private ClubeRepository repository;

    private ClubeService service;

    @BeforeEach
    void setUp() {
        service = new ClubeService(repository);
    }

    private Clube umClube(StatusClube status) {
        return Clube.builder()
                .nome("Clube A")
                .responsavel("Fulano")
                .telefone("88999999999")
                .endereco(new Endereco("Rua A", "10", "Centro", "Quixadá", "63900-000"))
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("deve buscar todos quando o nome for nulo")
        void deveBuscarTodosQuandoNomeNulo() {
            Page<Clube> esperado = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(esperado);

            Page<Clube> resultado = service.findAll(null, PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNomeContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("deve buscar todos quando o nome for em branco")
        void deveBuscarTodosQuandoNomeEmBranco() {
            Page<Clube> esperado = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(esperado);

            Page<Clube> resultado = service.findAll("   ", PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNomeContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("deve filtrar por nome quando informado")
        void deveFiltrarPorNomeQuandoInformado() {
            Page<Clube> esperado = Page.empty();
            when(repository.findByNomeContainingIgnoreCase("clube", PAGEABLE)).thenReturn(esperado);

            Page<Clube> resultado = service.findAll("clube", PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findByNomeContainingIgnoreCase("clube", PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("deve retornar o clube quando existir")
        void deveRetornarClubeQuandoExistir() {
            Clube clube = umClube(StatusClube.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(clube));

            Clube resultado = service.findByIdOrThrowNotFound(1L);

            assertThat(resultado).isSameAs(clube);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("deve lançar ClubeNotFoundException quando não existir")
        void deveLancarNotFoundQuandoNaoExistir() {
            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(ClubeNotFoundException.class)
                    .hasMessage("Clube não encontrado");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("deve forçar status ATIVO e salvar")
        void deveForcarStatusAtivoESalvar() {
            Clube clube = umClube(StatusClube.INATIVO);
            when(repository.save(any(Clube.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

            Clube salvo = service.save(clube);

            assertThat(salvo).isSameAs(clube);
            assertThat(salvo.getStatus()).isEqualTo(StatusClube.ATIVO);
            verify(repository).save(clube);
        }
    }

    @Nested
    @DisplayName("ativar / inativar")
    class AtivarInativar {

        @Test
        @DisplayName("deve ativar quando estiver INATIVO")
        void deveAtivarQuandoInativo() {
            Clube clube = umClube(StatusClube.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(clube));

            Clube resultado = service.ativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusClube.ATIVO);
            verify(repository, never()).save(any(Clube.class));
        }

        @Test
        @DisplayName("deve manter ATIVO quando já estiver ATIVO (idempotência)")
        void deveManterAtivoQuandoJaAtivo() {
            Clube clube = umClube(StatusClube.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(clube));

            Clube resultado = service.ativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusClube.ATIVO);
            verify(repository, never()).save(any(Clube.class));
        }

        @Test
        @DisplayName("deve lançar ClubeNotFoundException quando ativar id inexistente")
        void deveLancarNotFoundQuandoAtivarInexistente() {
            assertThatThrownBy(() -> service.ativar(1L))
                    .isInstanceOf(ClubeNotFoundException.class)
                    .hasMessage("Clube não encontrado");
        }

        @Test
        @DisplayName("deve inativar quando estiver ATIVO")
        void deveInativarQuandoAtivo() {
            Clube clube = umClube(StatusClube.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(clube));

            Clube resultado = service.inativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusClube.INATIVO);
            verify(repository, never()).save(any(Clube.class));
        }

        @Test
        @DisplayName("deve manter INATIVO quando já estiver INATIVO (idempotência)")
        void deveManterInativoQuandoJaInativo() {
            Clube clube = umClube(StatusClube.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(clube));

            Clube resultado = service.inativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusClube.INATIVO);
            verify(repository, never()).save(any(Clube.class));
        }

        @Test
        @DisplayName("deve lançar ClubeNotFoundException quando inativar id inexistente")
        void deveLancarNotFoundQuandoInativarInexistente() {
            assertThatThrownBy(() -> service.inativar(1L))
                    .isInstanceOf(ClubeNotFoundException.class)
                    .hasMessage("Clube não encontrado");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        private Clube novosDados() {
            return Clube.builder()
                    .nome("Clube B")
                    .responsavel("Beltrano")
                    .telefone("88888888888")
                    .endereco(new Endereco("Rua B", "20", "Bairro Novo", "Fortaleza", "60000-000"))
                    .build();
        }

        @Test
        @DisplayName("deve copiar os dados e não chamar save")
        void deveAtualizarDadosQuandoAtivo() {
            Clube existente = umClube(StatusClube.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            Clube resultado = service.update(1L, novosDados());

            assertThat(resultado).isSameAs(existente);
            assertThat(resultado.getNome()).isEqualTo("Clube B");
            assertThat(resultado.getResponsavel()).isEqualTo("Beltrano");
            assertThat(resultado.getTelefone()).isEqualTo("88888888888");
            assertThat(resultado.getEndereco().getCidade()).isEqualTo("Fortaleza");
            verify(repository, never()).save(any(Clube.class));
        }

        @Test
        @DisplayName("deve lançar ClubeInativoException quando o clube estiver INATIVO")
        void deveLancarInativoQuandoAtualizarInativo() {
            Clube existente = umClube(StatusClube.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.update(1L, novosDados()))
                    .isInstanceOf(ClubeInativoException.class)
                    .hasMessage("Clube está inativo, reative antes de editar");
        }

        @Test
        @DisplayName("deve lançar ClubeNotFoundException quando o id não existir")
        void deveLancarNotFoundQuandoAtualizarInexistente() {
            assertThatThrownBy(() -> service.update(1L, novosDados()))
                    .isInstanceOf(ClubeNotFoundException.class)
                    .hasMessage("Clube não encontrado");
        }
    }
}
