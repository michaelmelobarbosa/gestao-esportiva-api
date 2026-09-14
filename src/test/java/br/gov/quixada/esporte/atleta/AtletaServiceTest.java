package br.gov.quixada.esporte.atleta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import br.gov.quixada.esporte.atleta.exception.AtletaInativoException;
import br.gov.quixada.esporte.atleta.exception.AtletaNotFoundException;
import br.gov.quixada.esporte.atleta.exception.CpfJaCadastradoException;
import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.extras.Sexo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AtletaService")
class AtletaServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private AtletaRepository repository;

    private AtletaService service;

    @BeforeEach
    void setUp() {
        service = new AtletaService(repository);
    }

    private Atleta umAtleta(StatusAtleta status) {
        return Atleta.builder()
                .nomeCompleto("Fulano de Tal")
                .cpf("52998224725")
                .dataNascimento(LocalDate.of(2000, 1, 1))
                .endereco(new Endereco("Rua A", "10", "Centro", "Quixadá", "63900-000"))
                .telefone("88999999999")
                .sexo(Sexo.MASCULINO)
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @Order(1)
        @DisplayName("deve buscar todos quando o nome for nulo")
        void deveBuscarTodosQuandoNomeNulo() {
            Page<Atleta> esperado = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(esperado);

            Page<Atleta> resultado = service.findAll(null, PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNomeCompletoContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @Order(2)
        @DisplayName("deve buscar todos quando o nome for em branco")
        void deveBuscarTodosQuandoNomeEmBranco() {
            Page<Atleta> esperado = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(esperado);

            Page<Atleta> resultado = service.findAll("   ", PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNomeCompletoContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @Order(3)
        @DisplayName("deve filtrar por nome quando informado")
        void deveFiltrarPorNomeQuandoInformado() {
            Page<Atleta> esperado = new PageImpl<>(List.of(umAtleta(StatusAtleta.ATIVO)));
            when(repository.findByNomeCompletoContainingIgnoreCase("joao", PAGEABLE)).thenReturn(esperado);

            Page<Atleta> resultado = service.findAll("joao", PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findByNomeCompletoContainingIgnoreCase("joao", PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @Order(4)
        @DisplayName("deve retornar o atleta quando existir")
        void deveRetornarAtletaQuandoExistir() {
            Atleta atleta = umAtleta(StatusAtleta.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(atleta));

            Atleta resultado = service.findByIdOrThrowNotFound(1L);

            assertThat(resultado).isSameAs(atleta);
            verify(repository).findById(1L);
        }

        @Test
        @Order(5)
        @DisplayName("deve lançar AtletaNotFoundException quando não existir")
        void deveLancarNotFoundQuandoNaoExistir() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(AtletaNotFoundException.class)
                    .hasMessage("Atleta não encontrado");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @Order(6)
        @DisplayName("deve normalizar o CPF e forçar status ATIVO")
        void deveNormalizarCpfEForcarStatusAtivo() {
            Atleta atleta = Atleta.builder()
                    .nomeCompleto("Fulano de Tal")
                    .cpf("529.982.247-25")
                    .dataNascimento(LocalDate.of(2000, 1, 1))
                    .endereco(new Endereco("Rua A", "10", "Centro", "Quixadá", "63900-000"))
                    .telefone("88999999999")
                    .sexo(Sexo.MASCULINO)
                    .status(StatusAtleta.INATIVO)
                    .build();
            when(repository.save(any(Atleta.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

            Atleta salvo = service.save(atleta);

            assertThat(salvo).isSameAs(atleta);
            assertThat(salvo.getCpf()).isEqualTo("52998224725");
            assertThat(salvo.getStatus()).isEqualTo(StatusAtleta.ATIVO);
            verify(repository).save(atleta);
        }

        @Test
        @Order(7)
        @DisplayName("deve lançar CpfJaCadastradoException quando o erro for 1062")
        void deveLancarCpfJaCadastradoQuandoErro1062() {
            Atleta atleta = umAtleta(StatusAtleta.ATIVO);
            SQLIntegrityConstraintViolationException causa =
                    new SQLIntegrityConstraintViolationException("Duplicate entry", "23000", 1062);
            when(repository.save(any(Atleta.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicado", causa));

            assertThatThrownBy(() -> service.save(atleta))
                    .isInstanceOf(CpfJaCadastradoException.class)
                    .hasMessage("CPF já cadastrado");
        }

        @Test
        @Order(8)
        @DisplayName("deve relançar quando o código MySQL não for 1062")
        void deveRelancarQuandoCodigoNaoFor1062() {
            Atleta atleta = umAtleta(StatusAtleta.ATIVO);
            SQLIntegrityConstraintViolationException causa =
                    new SQLIntegrityConstraintViolationException("Foreign key constraint fails", "2300", 1452);
            DataIntegrityViolationException excecao =
                    new DataIntegrityViolationException("conflito", causa);
            when(repository.save(any(Atleta.class))).thenThrow(excecao);

            assertThatThrownBy(() -> service.save(atleta))
                    .isSameAs(excecao);
        }

        @Test
        @Order(9)
        @DisplayName("deve relançar quando a causa não for SQLIntegrityConstraintViolationException")
        void deveRelancarQuandoCausaNaoForSql() {
            Atleta atleta = umAtleta(StatusAtleta.ATIVO);
            DataIntegrityViolationException excecao =
                    new DataIntegrityViolationException("conflito", new RuntimeException("outro erro"));
            when(repository.save(any(Atleta.class))).thenThrow(excecao);

            assertThatThrownBy(() -> service.save(atleta))
                    .isSameAs(excecao);
        }
    }

    @Nested
    @DisplayName("ativar / inativar")
    class AtivarInativar {

        @Test
        @Order(10)
        @DisplayName("deve ativar quando estiver INATIVO")
        void deveAtivarQuandoInativo() {
            Atleta atleta = umAtleta(StatusAtleta.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(atleta));

            Atleta resultado = service.ativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusAtleta.ATIVO);
            verify(repository, never()).save(any(Atleta.class));
        }

        @Test
        @Order(11)
        @DisplayName("deve manter ATIVO quando já estiver ATIVO (idempotência)")
        void deveManterAtivoQuandoJaAtivo() {
            Atleta atleta = umAtleta(StatusAtleta.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(atleta));

            Atleta resultado = service.ativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusAtleta.ATIVO);
            verify(repository, never()).save(any(Atleta.class));
        }

        @Test
        @Order(12)
        @DisplayName("deve lançar AtletaNotFoundException quando ativar id inexistente")
        void deveLancarNotFoundQuandoAtivarInexistente() {
            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.ativar(1L))
                    .isInstanceOf(AtletaNotFoundException.class);
        }

        @Test
        @Order(13)
        @DisplayName("deve inativar quando estiver ATIVO")
        void deveInativarQuandoAtivo() {
            Atleta atleta = umAtleta(StatusAtleta.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(atleta));

            Atleta resultado = service.inativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusAtleta.INATIVO);
            verify(repository, never()).save(any(Atleta.class));
        }

        @Test
        @Order(14)
        @DisplayName("deve manter INATIVO quando já estiver INATIVO (idempotência)")
        void deveManterInativoQuandoJaInativo() {
            Atleta atleta = umAtleta(StatusAtleta.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(atleta));

            Atleta resultado = service.inativar(1L);

            assertThat(resultado.getStatus()).isEqualTo(StatusAtleta.INATIVO);
            verify(repository, never()).save(any(Atleta.class));
        }

        @Test
        @Order(15)
        @DisplayName("deve lançar AtletaNotFoundException quando inativar id inexistente")
        void deveLancarNotFoundQuandoInativarInexistente() {
            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.inativar(1L))
                    .isInstanceOf(AtletaNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        private Atleta novosDados() {
            return Atleta.builder()
                    .nomeCompleto("Novo Nome")
                    .cpf("11111111111")
                    .dataNascimento(LocalDate.of(1990, 5, 5))
                    .endereco(new Endereco("Rua B", "20", "Bairro Novo", "Fortaleza", "60000-000"))
                    .telefone("88888888888")
                    .sexo(Sexo.FEMININO)
                    .build();
        }

        @Test
        @Order(16)
        @DisplayName("deve copiar os dados e retornar a mesma instância sem save")
        void deveAtualizarDadosQuandoAtivo() {
            Atleta existente = umAtleta(StatusAtleta.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            Atleta resultado = service.update(1L, novosDados());

            assertThat(resultado).isSameAs(existente);
            assertThat(resultado.getNomeCompleto()).isEqualTo("Novo Nome");
            assertThat(resultado.getDataNascimento()).isEqualTo(LocalDate.of(1990, 5, 5));
            assertThat(resultado.getEndereco().getCidade()).isEqualTo("Fortaleza");
            assertThat(resultado.getTelefone()).isEqualTo("88888888888");
            assertThat(resultado.getSexo()).isEqualTo(Sexo.FEMININO);
            verify(repository, never()).save(any(Atleta.class));
        }

        @Test
        @Order(17)
        @DisplayName("não deve alterar o CPF (imutável após criação)")
        void naoDeveAlterarCpf() {
            Atleta existente = umAtleta(StatusAtleta.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            Atleta resultado = service.update(1L, novosDados());

            assertThat(resultado.getCpf()).isEqualTo("52998224725");
        }

        @Test
        @Order(18)
        @DisplayName("deve sobrescrever campos com nulos (substituição total; DTO @Valid impede isso via API)")
        void deveSobrescreverCamposNulos() {
            Atleta existente = umAtleta(StatusAtleta.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            Atleta resultado = service.update(1L, Atleta.builder().build());

            assertThat(resultado).isSameAs(existente);
            assertThat(resultado.getNomeCompleto()).isNull();
            assertThat(resultado.getDataNascimento()).isNull();
            assertThat(resultado.getEndereco()).isNull();
            assertThat(resultado.getTelefone()).isNull();
            assertThat(resultado.getSexo()).isNull();
            assertThat(resultado.getCpf()).isEqualTo("52998224725");
        }

        @Test
        @Order(19)
        @DisplayName("deve lançar AtletaInativoException quando o atleta estiver INATIVO")
        void deveLancarInativoQuandoAtualizarInativo() {
            Atleta existente = umAtleta(StatusAtleta.INATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.update(1L, novosDados()))
                    .isInstanceOf(AtletaInativoException.class)
                    .hasMessage("Atleta está inativo, reative antes de editar");
        }

        @Test
        @Order(20)
        @DisplayName("deve lançar AtletaNotFoundException quando o id não existir")
        void deveLancarNotFoundQuandoAtualizarInexistente() {
            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(1L, novosDados()))
                    .isInstanceOf(AtletaNotFoundException.class);
        }
    }
}
