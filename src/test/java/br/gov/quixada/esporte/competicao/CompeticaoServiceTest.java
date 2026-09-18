package br.gov.quixada.esporte.competicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import br.gov.quixada.esporte.competicao.exception.CompeticaoNaoEditavelException;
import br.gov.quixada.esporte.competicao.exception.CompeticaoNotFoundException;
import br.gov.quixada.esporte.competicao.exception.PeriodoInvalidoException;
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
@DisplayName("CompeticaoService")
class CompeticaoServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private CompeticaoRepository repository;

    @Mock
    private ModalidadeRepository modalidadeRepository;

    private CompeticaoService service;

    @BeforeEach
    void setUp() {
        service = new CompeticaoService(repository, modalidadeRepository);
    }

    private Competicao umaCompeticao(StatusCompeticao status) {
        return Competicao.builder()
                .nome("Copa Teste")
                .ano(2026)
                .dataInicio(LocalDate.of(2026, 1, 1))
                .dataFim(LocalDate.of(2026, 1, 31))
                .status(status)
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
            Page<Competicao> esperado = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(esperado);

            Page<Competicao> resultado = service.findAll(null, PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNomeContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("deve buscar todos quando o nome for em branco")
        void deveBuscarTodosQuandoNomeEmBranco() {
            Page<Competicao> esperado = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(esperado);

            Page<Competicao> resultado = service.findAll("   ", PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNomeContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("deve filtrar por nome quando informado")
        void deveFiltrarPorNomeQuandoInformado() {
            Page<Competicao> esperado = Page.empty();
            when(repository.findByNomeContainingIgnoreCase("copa", PAGEABLE)).thenReturn(esperado);

            Page<Competicao> resultado = service.findAll("copa", PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findByNomeContainingIgnoreCase("copa", PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("deve retornar a competição quando existir")
        void deveRetornarCompeticaoQuandoExistir() {
            Competicao competicao = umaCompeticao(StatusCompeticao.PLANEJAMENTO);
            when(repository.findById(1L)).thenReturn(Optional.of(competicao));

            Competicao resultado = service.findByIdOrThrowNotFound(1L);

            assertThat(resultado).isSameAs(competicao);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("deve lançar CompeticaoNotFoundException quando não existir")
        void deveLancarNotFoundQuandoNaoExistir() {
            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(CompeticaoNotFoundException.class)
                    .hasMessage("Competição não encontrada");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("deve validar período, definir modalidade e salvar")
        void deveSalvarComModalidade() {
            Competicao competicao = umaCompeticao(StatusCompeticao.PLANEJAMENTO);
            Modalidade modalidade = umaModalidade();
            when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(modalidade));
            when(repository.save(any(Competicao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

            Competicao salva = service.save(competicao, 1L);

            assertThat(salva).isSameAs(competicao);
            assertThat(salva.getModalidade()).isSameAs(modalidade);
            verify(repository).save(competicao);
        }

        @Test
        @DisplayName("deve lançar PeriodoInvalidoException quando dataFim não for posterior a dataInicio")
        void deveLancarPeriodoInvalido() {
            Competicao competicao = Competicao.builder()
                    .nome("Copa Teste")
                    .ano(2026)
                    .dataInicio(LocalDate.of(2026, 1, 31))
                    .dataFim(LocalDate.of(2026, 1, 1))
                    .build();

            assertThatThrownBy(() -> service.save(competicao, 1L))
                    .isInstanceOf(PeriodoInvalidoException.class)
                    .hasMessage("Data final deve ser posterior à data inicial");
        }

        @Test
        @DisplayName("deve lançar ModalidadeNotFoundException quando a modalidade não existir")
        void deveLancarNotFoundQuandoModalidadeNaoExistir() {
            Competicao competicao = umaCompeticao(StatusCompeticao.PLANEJAMENTO);

            assertThatThrownBy(() -> service.save(competicao, 99L))
                    .isInstanceOf(ModalidadeNotFoundException.class)
                    .hasMessage("Modalidade não encontrada");
        }
    }

    @Nested
    @DisplayName("alterarStatus")
    class AlterarStatus {

        @Test
        @DisplayName("deve alterar o status e não chamar save")
        void deveAlterarStatus() {
            Competicao competicao = umaCompeticao(StatusCompeticao.PLANEJAMENTO);
            when(repository.findById(1L)).thenReturn(Optional.of(competicao));

            Competicao resultado = service.alterarStatus(1L, StatusCompeticao.INSCRICOES_ABERTAS);

            assertThat(resultado.getStatus()).isEqualTo(StatusCompeticao.INSCRICOES_ABERTAS);
            verify(repository, never()).save(any(Competicao.class));
        }

        @Test
        @DisplayName("deve manter o status quando for o mesmo (idempotência)")
        void deveManterStatusQuandoIgual() {
            Competicao competicao = umaCompeticao(StatusCompeticao.EM_ANDAMENTO);
            when(repository.findById(1L)).thenReturn(Optional.of(competicao));

            Competicao resultado = service.alterarStatus(1L, StatusCompeticao.EM_ANDAMENTO);

            assertThat(resultado.getStatus()).isEqualTo(StatusCompeticao.EM_ANDAMENTO);
            verify(repository, never()).save(any(Competicao.class));
        }

        @Test
        @DisplayName("deve lançar CompeticaoNotFoundException quando o id não existir")
        void deveLancarNotFoundQuandoInexistente() {
            assertThatThrownBy(() -> service.alterarStatus(1L, StatusCompeticao.EM_ANDAMENTO))
                    .isInstanceOf(CompeticaoNotFoundException.class)
                    .hasMessage("Competição não encontrada");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        private Competicao novosDados() {
            return Competicao.builder()
                    .nome("Copa Nova")
                    .ano(2027)
                    .dataInicio(LocalDate.of(2027, 2, 1))
                    .dataFim(LocalDate.of(2027, 2, 28))
                    .build();
        }

        @Test
        @DisplayName("deve copiar nome, ano e datas e não chamar save")
        void deveAtualizarDadosQuandoEditavel() {
            Competicao existente = umaCompeticao(StatusCompeticao.PLANEJAMENTO);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            Competicao resultado = service.update(1L, novosDados());

            assertThat(resultado).isSameAs(existente);
            assertThat(resultado.getNome()).isEqualTo("Copa Nova");
            assertThat(resultado.getAno()).isEqualTo(2027);
            assertThat(resultado.getDataInicio()).isEqualTo(LocalDate.of(2027, 2, 1));
            assertThat(resultado.getDataFim()).isEqualTo(LocalDate.of(2027, 2, 28));
            verify(repository, never()).save(any(Competicao.class));
        }

        @Test
        @DisplayName("deve lançar CompeticaoNaoEditavelException quando estiver FINALIZADA")
        void deveLancarNaoEditavelQuandoFinalizada() {
            Competicao existente = umaCompeticao(StatusCompeticao.FINALIZADA);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.update(1L, novosDados()))
                    .isInstanceOf(CompeticaoNaoEditavelException.class)
                    .hasMessage("Competição finalizada ou cancelada não pode ser editada");
        }

        @Test
        @DisplayName("deve lançar CompeticaoNaoEditavelException quando estiver CANCELADA")
        void deveLancarNaoEditavelQuandoCancelada() {
            Competicao existente = umaCompeticao(StatusCompeticao.CANCELADA);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.update(1L, novosDados()))
                    .isInstanceOf(CompeticaoNaoEditavelException.class)
                    .hasMessage("Competição finalizada ou cancelada não pode ser editada");
        }

        @Test
        @DisplayName("deve lançar PeriodoInvalidoException quando as datas forem inválidas")
        void deveLancarPeriodoInvalido() {
            Competicao existente = umaCompeticao(StatusCompeticao.PLANEJAMENTO);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));
            Competicao novos = Competicao.builder()
                    .nome("Copa Nova")
                    .ano(2027)
                    .dataInicio(LocalDate.of(2027, 2, 28))
                    .dataFim(LocalDate.of(2027, 2, 1))
                    .build();

            assertThatThrownBy(() -> service.update(1L, novos))
                    .isInstanceOf(PeriodoInvalidoException.class)
                    .hasMessage("Data final deve ser posterior à data inicial");
        }

        @Test
        @DisplayName("deve lançar CompeticaoNotFoundException quando o id não existir")
        void deveLancarNotFoundQuandoInexistente() {
            assertThatThrownBy(() -> service.update(1L, novosDados()))
                    .isInstanceOf(CompeticaoNotFoundException.class)
                    .hasMessage("Competição não encontrada");
        }
    }
}
