package br.gov.quixada.esporte.inscricao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StatusInscricao")
class StatusInscricaoTest {

    @Test
    @DisplayName("deve indicar quais status ocupam e quais liberam vaga")
    void deveIndicarOcupacaoDeVaga() {
        assertThat(StatusInscricao.PENDENTE.ocupaVaga()).isTrue();
        assertThat(StatusInscricao.APROVADA.ocupaVaga()).isTrue();
        assertThat(StatusInscricao.INATIVA.ocupaVaga()).isTrue();
        assertThat(StatusInscricao.CANCELADA.liberaVaga()).isTrue();
        assertThat(StatusInscricao.REJEITADA.liberaVaga()).isTrue();
    }

    @Test
    @DisplayName("deve permitir transições válidas")
    void devePermitirTransicoesValidas() {
        assertThat(StatusInscricao.PENDENTE.podeTransicionarPara(StatusInscricao.APROVADA)).isTrue();
        assertThat(StatusInscricao.PENDENTE.podeTransicionarPara(StatusInscricao.REJEITADA)).isTrue();
        assertThat(StatusInscricao.APROVADA.podeTransicionarPara(StatusInscricao.CANCELADA)).isTrue();
        assertThat(StatusInscricao.INATIVA.podeTransicionarPara(StatusInscricao.PENDENTE)).isTrue();
        assertThat(StatusInscricao.REJEITADA.podeTransicionarPara(StatusInscricao.PENDENTE)).isTrue();
    }

    @Test
    @DisplayName("deve bloquear transições inválidas")
    void deveBloquearTransicoesInvalidas() {
        assertThat(StatusInscricao.CANCELADA.podeTransicionarPara(StatusInscricao.PENDENTE)).isFalse();
        assertThat(StatusInscricao.APROVADA.podeTransicionarPara(StatusInscricao.PENDENTE)).isFalse();
        assertThat(StatusInscricao.REJEITADA.podeTransicionarPara(StatusInscricao.APROVADA)).isFalse();
    }
}
