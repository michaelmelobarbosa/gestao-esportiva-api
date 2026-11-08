package br.gov.quixada.esporte.partida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.gov.quixada.esporte.categoria.Categoria;
import br.gov.quixada.esporte.competicao.Competicao;
import br.gov.quixada.esporte.competicao.StatusCompeticao;
import br.gov.quixada.esporte.equipe.Equipe;
import br.gov.quixada.esporte.equipe.StatusEquipe;
import br.gov.quixada.esporte.inscricao.InscricaoEquipe;
import br.gov.quixada.esporte.inscricao.InscricaoEquipeRepository;
import br.gov.quixada.esporte.inscricao.StatusInscricao;
import br.gov.quixada.esporte.partida.dto.PartidaCreateRequest;
import br.gov.quixada.esporte.partida.dto.PartidaResultadoRequest;
import br.gov.quixada.esporte.partida.exception.PartidaInvalidaException;
import br.gov.quixada.esporte.partida.exception.TransicaoStatusPartidaException;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartidaService")
class PartidaServiceTest {

    private static final LocalDateTime DATA_HORA = LocalDateTime.of(2026, 7, 1, 15, 0);

    @Mock
    private PartidaRepository repository;

    @Mock
    private InscricaoEquipeRepository inscricaoEquipeRepository;

    private PartidaService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Fortaleza"));
        service = new PartidaService(repository, inscricaoEquipeRepository, clock);
    }

    private InscricaoEquipe inscricao(Long id, StatusInscricao status, Long categoriaId, String equipeNome) {
        var competicao = Competicao.builder()
                .id(1L)
                .nome("Copa Teste")
                .dataInicioInscricao(LocalDate.of(2026, 1, 1))
                .dataFimInscricao(LocalDate.of(2026, 2, 1))
                .dataInicio(LocalDate.of(2026, 3, 1))
                .dataFim(LocalDate.of(2026, 12, 31))
                .status(StatusCompeticao.INSCRICOES_ABERTAS)
                .build();
        var categoria = Categoria.builder()
                .id(categoriaId)
                .nome("Sub-20")
                .competicao(competicao)
                .build();
        var equipe = Equipe.builder()
                .id(id)
                .nome(equipeNome)
                .status(StatusEquipe.ATIVO)
                .build();
        return InscricaoEquipe.builder()
                .id(id)
                .status(status)
                .competicao(competicao)
                .categoria(categoria)
                .equipe(equipe)
                .build();
    }

    private void stubCriacaoSemConflito() {
        when(repository.existsConfrontoNaRodada(anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(false);
        when(repository.existsConflitoDataHora(any(), any(), anyLong(), any())).thenReturn(false);
        when(repository.save(any(Partida.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("deve criar partida com inscrições aprovadas da mesma competição e categoria")
    void deveCriarPartidaValida() {
        var mandante = inscricao(1L, StatusInscricao.APROVADA, 10L, "Equipe A");
        var visitante = inscricao(2L, StatusInscricao.APROVADA, 10L, "Equipe B");
        when(inscricaoEquipeRepository.findById(1L)).thenReturn(Optional.of(mandante));
        when(inscricaoEquipeRepository.findById(2L)).thenReturn(Optional.of(visitante));
        stubCriacaoSemConflito();

        var partida = service.create(new PartidaCreateRequest(1L, 2L, 1, DATA_HORA, "Ginásio"));

        assertThat(partida.getStatus()).isEqualTo(StatusPartida.AGENDADA);
        assertThat(partida.getInscricaoMandante()).isSameAs(mandante);
        assertThat(partida.getInscricaoVisitante()).isSameAs(visitante);
    }

    @Test
    @DisplayName("deve rejeitar equipes com inscrição não aprovada")
    void deveRejeitarInscricaoNaoAprovada() {
        var mandante = inscricao(1L, StatusInscricao.PENDENTE, 10L, "Equipe A");
        var visitante = inscricao(2L, StatusInscricao.APROVADA, 10L, "Equipe B");
        when(inscricaoEquipeRepository.findById(1L)).thenReturn(Optional.of(mandante));
        when(inscricaoEquipeRepository.findById(2L)).thenReturn(Optional.of(visitante));

        assertThatThrownBy(() -> service.create(new PartidaCreateRequest(1L, 2L, 1, DATA_HORA, null)))
                .isInstanceOf(PartidaInvalidaException.class)
                .hasMessage("As duas equipes precisam ter inscrição APROVADA");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deve rejeitar mandante e visitante iguais")
    void deveRejeitarEquipesIguais() {
        var mandante = inscricao(1L, StatusInscricao.APROVADA, 10L, "Equipe A");
        when(inscricaoEquipeRepository.findById(1L)).thenReturn(Optional.of(mandante));

        assertThatThrownBy(() -> service.create(new PartidaCreateRequest(1L, 1L, 1, DATA_HORA, null)))
                .isInstanceOf(PartidaInvalidaException.class)
                .hasMessage("Mandante e visitante devem ser diferentes");
    }

    @Test
    @DisplayName("deve iniciar partida agendada")
    void deveIniciarPartida() {
        var partida = Partida.builder().id(1L).status(StatusPartida.AGENDADA).build();
        when(repository.findById(1L)).thenReturn(Optional.of(partida));

        var resultado = service.alterarStatus(1L, StatusPartida.EM_ANDAMENTO);

        assertThat(resultado.getStatus()).isEqualTo(StatusPartida.EM_ANDAMENTO);
    }

    @Test
    @DisplayName("deve bloquear transição inválida")
    void deveBloquearTransicaoInvalida() {
        var partida = Partida.builder().id(1L).status(StatusPartida.CANCELADA).build();
        when(repository.findById(1L)).thenReturn(Optional.of(partida));

        assertThatThrownBy(() -> service.alterarStatus(1L, StatusPartida.EM_ANDAMENTO))
                .isInstanceOf(TransicaoStatusPartidaException.class);
    }

    @Test
    @DisplayName("deve registrar resultado e finalizar partida em andamento")
    void deveRegistrarResultado() {
        var partida = Partida.builder().id(1L).status(StatusPartida.EM_ANDAMENTO).build();
        when(repository.findById(1L)).thenReturn(Optional.of(partida));

        var resultado = service.registrarResultado(1L, new PartidaResultadoRequest(2, 1));

        assertThat(resultado.getStatus()).isEqualTo(StatusPartida.FINALIZADA);
        assertThat(resultado.getGolsMandante()).isEqualTo(2);
        assertThat(resultado.getGolsVisitante()).isEqualTo(1);
    }

    @Test
    @DisplayName("deve rejeitar resultado de partida ainda agendada")
    void deveRejeitarResultadoDePartidaAgendada() {
        var partida = Partida.builder().id(1L).status(StatusPartida.AGENDADA).build();
        when(repository.findById(1L)).thenReturn(Optional.of(partida));

        assertThatThrownBy(() -> service.registrarResultado(1L, new PartidaResultadoRequest(1, 0)))
                .isInstanceOf(PartidaInvalidaException.class)
                .hasMessage("Só é possível registrar resultado de partida EM_ANDAMENTO");
    }
}
