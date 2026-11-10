package br.gov.quixada.esporte.competicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import br.gov.quixada.esporte.competicao.exception.CompeticaoNaoEditavelException;
import br.gov.quixada.esporte.competicao.exception.CompeticaoNotFoundException;
import br.gov.quixada.esporte.competicao.exception.PeriodoInvalidoException;
import br.gov.quixada.esporte.competicao.exception.TransicaoStatusCompeticaoException;
import br.gov.quixada.esporte.inscricao.InscricaoEquipeService;
import br.gov.quixada.esporte.modalidade.StatusModalidade;
import br.gov.quixada.esporte.modalidade.exception.ModalidadeInativaException;
import br.gov.quixada.esporte.modalidade.Modalidade;
import br.gov.quixada.esporte.partida.PartidaService;
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

    @Mock
    private InscricaoEquipeService inscricaoEquipeService;

    @Mock
    private PartidaService partidaService;

    private CompeticaoService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Fortaleza"));
        service = new CompeticaoService(repository, modalidadeRepository, inscricaoEquipeService, partidaService, clock);
    }

    private Competicao umaCompeticao(StatusCompeticao status) {
        return Competicao.builder()
                .nome("Copa Teste")
                .ano(2026)
                .dataInicioInscricao(LocalDate.of(2025, 11, 1))
                .dataFimInscricao(LocalDate.of(2025, 12, 1))
                .dataInicio(LocalDate.of(2026, 1, 1))
                .dataFim(LocalDate.of(2026, 1, 31))
                .status(status)
                .build();
    }

    private Modalidade umaModalidade() {
        return Modalidade.builder()
                .nome("Futebol")
                .status(br.gov.quixada.esporte.modalidade.StatusModalidade.ATIVO)
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

            Page<Competicao> resultado = service.findAll(null, null, PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNomeContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("deve buscar todos quando o nome for em branco")
        void deveBuscarTodosQuandoNomeEmBranco() {
            Page<Competicao> esperado = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(esperado);

            Page<Competicao> resultado = service.findAll("   ", null, PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNomeContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("deve filtrar por nome quando informado")
        void deveFiltrarPorNomeQuandoInformado() {
            Page<Competicao> esperado = Page.empty();
            when(repository.findByNomeContainingIgnoreCase("copa", PAGEABLE)).thenReturn(esperado);

            Page<Competicao> resultado = service.findAll("copa", null, PAGEABLE);

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
        @DisplayName("deve cancelar e propagar o cancelamento para as inscrições")
        void deveCancelarCompeticao() {
            Competicao competicao = umaCompeticao(StatusCompeticao.PLANEJAMENTO);
            when(repository.findById(1L)).thenReturn(Optional.of(competicao));

            Competicao resultado = service.alterarStatus(1L, StatusCompeticao.CANCELADA);

            assertThat(resultado.getStatus()).isEqualTo(StatusCompeticao.CANCELADA);
            verify(inscricaoEquipeService).cancelarPorCompeticao(1L);
            verify(partidaService).cancelarPorCompeticao(1L);
        }

        @Test
        @DisplayName("deve proibir alteração manual de status que não seja CANCELADA")
        void deveProibirStatusManual() {
            assertThatThrownBy(() -> service.alterarStatus(1L, StatusCompeticao.INSCRICOES_ABERTAS))
                    .isInstanceOf(TransicaoStatusCompeticaoException.class);
        }

        @Test
        @DisplayName("deve lançar CompeticaoNotFoundException quando o id não existir")
        void deveLancarNotFoundQuandoInexistente() {
            assertThatThrownBy(() -> service.alterarStatus(1L, StatusCompeticao.CANCELADA))
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
                    .dataInicioInscricao(LocalDate.of(2026, 10, 1))
                    .dataFimInscricao(LocalDate.of(2026, 11, 1))
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

    @Nested
    @DisplayName("regras de negócio")
    class RegrasDeNegocio {

        @Test
        @DisplayName("deve bloquear criação com modalidade inativa")
        void deveBloquearModalidadeInativa() {
            Modalidade modalidade = Modalidade.builder().id(1L).nome("Futebol")
                    .status(StatusModalidade.INATIVO).build();
            when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(modalidade));

            assertThatThrownBy(() -> service.save(umaCompeticao(StatusCompeticao.PLANEJAMENTO), 1L))
                    .isInstanceOf(ModalidadeInativaException.class);
        }

        @Test
        @DisplayName("deve recalcular o status ao criar com base nas datas")
        void deveRecalcularStatusAoCriar() {
            Modalidade modalidade = umaModalidade();
            when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(modalidade));
            when(repository.save(any(Competicao.class))).thenAnswer(invocation -> invocation.getArgument(0));
            Competicao competicao = umaCompeticao(StatusCompeticao.PLANEJAMENTO);

            Competicao salva = service.save(competicao, 1L);

            assertThat(salva.getStatus()).isEqualTo(StatusCompeticao.FINALIZADA);
        }
    }

    @Nested
    @DisplayName("dataCancelamento")
    class DataCancelamento {

        @Test
        @DisplayName("deve registrar a data ao cancelar")
        void deveRegistrarDataAoCancelar() {
            Competicao competicao = umaCompeticao(StatusCompeticao.PLANEJAMENTO);
            when(repository.findById(1L)).thenReturn(Optional.of(competicao));

            Competicao resultado = service.alterarStatus(1L, StatusCompeticao.CANCELADA);

            assertThat(resultado.getDataCancelamento()).isNotNull();
        }
    }

    @Nested
    @DisplayName("atualizarObservacao")
    class AtualizarObservacao {

        @Test
        @DisplayName("deve gravar a observação livre")
        void deveGravarObservacao() {
            Competicao competicao = umaCompeticao(StatusCompeticao.EM_ANDAMENTO);
            when(repository.findById(1L)).thenReturn(Optional.of(competicao));

            Competicao resultado = service.atualizarObservacao(1L, "Fim prorrogado por chuva");

            assertThat(resultado.getObservacao()).isEqualTo("Fim prorrogado por chuva");
        }

        @Test
        @DisplayName("deve permitir observação em competição CANCELADA")
        void devePermitirEmCompeticaoCancelada() {
            Competicao competicao = umaCompeticao(StatusCompeticao.CANCELADA);
            when(repository.findById(1L)).thenReturn(Optional.of(competicao));

            Competicao resultado = service.atualizarObservacao(1L, "Cancelada por falta de equipes");

            assertThat(resultado.getObservacao()).isEqualTo("Cancelada por falta de equipes");
        }

        @Test
        @DisplayName("deve limpar a observação quando vier nula")
        void deveLimparObservacao() {
            Competicao competicao = umaCompeticao(StatusCompeticao.PLANEJAMENTO);
            competicao.atualizarObservacao("texto antigo");
            when(repository.findById(1L)).thenReturn(Optional.of(competicao));

            Competicao resultado = service.atualizarObservacao(1L, null);

            assertThat(resultado.getObservacao()).isNull();
        }

        @Test
        @DisplayName("deve lançar not found quando a competição não existir")
        void deveLancarNotFound() {
            when(repository.findById(9L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.atualizarObservacao(9L, "x"))
                    .isInstanceOf(CompeticaoNotFoundException.class);
        }
    }
}
