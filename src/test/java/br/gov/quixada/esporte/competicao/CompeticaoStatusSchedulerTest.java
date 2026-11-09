package br.gov.quixada.esporte.competicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompeticaoStatusScheduler")
class CompeticaoStatusSchedulerTest {

    @Mock
    private CompeticaoRepository repository;

    private CompeticaoStatusScheduler scheduler;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Fortaleza"));
        scheduler = new CompeticaoStatusScheduler(repository, clock);
    }

    private Competicao competicao(StatusCompeticao status) {
        return Competicao.builder()
                .nome("Copa")
                .dataInicioInscricao(LocalDate.of(2026, 1, 1))
                .dataFimInscricao(LocalDate.of(2026, 2, 1))
                .dataInicio(LocalDate.of(2026, 3, 1))
                .dataFim(LocalDate.of(2026, 4, 1))
                .status(status)
                .build();
    }

    @Test
    @DisplayName("deve sincronizar status pelas datas e preservar CANCELADA")
    void deveSincronizar() {
        var encerrada = competicao(StatusCompeticao.PLANEJAMENTO);
        var cancelada = competicao(StatusCompeticao.CANCELADA);
        when(repository.findAll()).thenReturn(List.of(encerrada, cancelada));

        scheduler.sincronizar();

        assertThat(encerrada.getStatus()).isEqualTo(StatusCompeticao.FINALIZADA);
        assertThat(cancelada.getStatus()).isEqualTo(StatusCompeticao.CANCELADA);
    }
}
