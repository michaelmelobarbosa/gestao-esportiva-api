package br.gov.quixada.esporte.competicao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Competicao — cálculo de status")
class CompeticaoStatusTest {

    private static final LocalDate INSCRICAO_INICIO = LocalDate.of(2026, 1, 1);
    private static final LocalDate INSCRICAO_FIM = LocalDate.of(2026, 2, 1);
    private static final LocalDate EVENTO_INICIO = LocalDate.of(2026, 3, 1);
    private static final LocalDate EVENTO_FIM = LocalDate.of(2026, 4, 1);

    private Competicao competicao(StatusCompeticao status) {
        return Competicao.builder()
                .dataInicioInscricao(INSCRICAO_INICIO)
                .dataFimInscricao(INSCRICAO_FIM)
                .dataInicio(EVENTO_INICIO)
                .dataFim(EVENTO_FIM)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("deve calcular PLANEJAMENTO antes das inscrições")
    void planejamento() {
        assertThat(competicao(StatusCompeticao.PLANEJAMENTO).calcularStatus(LocalDate.of(2025, 12, 31)))
                .isEqualTo(StatusCompeticao.PLANEJAMENTO);
    }

    @Test
    @DisplayName("deve calcular INSCRICOES_ABERTAS no intervalo de inscrição")
    void inscricoesAbertas() {
        assertThat(competicao(StatusCompeticao.PLANEJAMENTO).calcularStatus(INSCRICAO_INICIO))
                .isEqualTo(StatusCompeticao.INSCRICOES_ABERTAS);
        assertThat(competicao(StatusCompeticao.PLANEJAMENTO).calcularStatus(INSCRICAO_FIM))
                .isEqualTo(StatusCompeticao.INSCRICOES_ABERTAS);
    }

    @Test
    @DisplayName("deve calcular INSCRICOES_ENCERRADAS entre inscrição e evento")
    void inscricoesEncerradas() {
        assertThat(competicao(StatusCompeticao.PLANEJAMENTO).calcularStatus(LocalDate.of(2026, 2, 2)))
                .isEqualTo(StatusCompeticao.INSCRICOES_ENCERRADAS);
    }

    @Test
    @DisplayName("deve calcular EM_ANDAMENTO durante o evento")
    void emAndamento() {
        assertThat(competicao(StatusCompeticao.PLANEJAMENTO).calcularStatus(EVENTO_INICIO))
                .isEqualTo(StatusCompeticao.EM_ANDAMENTO);
        assertThat(competicao(StatusCompeticao.PLANEJAMENTO).calcularStatus(EVENTO_FIM))
                .isEqualTo(StatusCompeticao.EM_ANDAMENTO);
    }

    @Test
    @DisplayName("deve calcular FINALIZADA após o evento")
    void finalizada() {
        assertThat(competicao(StatusCompeticao.PLANEJAMENTO).calcularStatus(EVENTO_FIM.plusDays(1)))
                .isEqualTo(StatusCompeticao.FINALIZADA);
    }

    @Test
    @DisplayName("CANCELADA é terminal")
    void canceladaTerminal() {
        assertThat(competicao(StatusCompeticao.CANCELADA).calcularStatus(EVENTO_FIM.plusDays(1)))
                .isEqualTo(StatusCompeticao.CANCELADA);
    }
}
