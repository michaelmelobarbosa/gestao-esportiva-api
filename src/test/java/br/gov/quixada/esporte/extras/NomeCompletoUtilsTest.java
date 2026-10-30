package br.gov.quixada.esporte.extras;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NomeCompletoUtils")
class NomeCompletoUtilsTest {

    @Test
    @DisplayName("deve remover acento, caixa e espaços")
    void deveRemoverAcentoCaixaEEspacos() {
        assertThat(NomeCompletoUtils.normalize("João Silva")).isEqualTo("joaosilva");
    }

    @Test
    @DisplayName("deve normalizar cedilha")
    void deveNormalizarCedilha() {
        assertThat(NomeCompletoUtils.normalize("Conceição")).isEqualTo("conceicao");
    }

    @Test
    @DisplayName("deve retornar vazio quando houver apenas símbolos e espaços")
    void deveRetornarVazioQuandoSomenteSimbolos() {
        assertThat(NomeCompletoUtils.normalize("@#$ %")).isEmpty();
    }

    @Test
    @DisplayName("deve ser idempotente para entrada já normalizada")
    void deveSerIdempotente() {
        assertThat(NomeCompletoUtils.normalize("joaosilva")).isEqualTo("joaosilva");
    }

    @Test
    @DisplayName("deve lançar NullPointerException quando o nome for nulo")
    void deveLancarNpeQuandoNomeNulo() {
        assertThatThrownBy(() -> NomeCompletoUtils.normalize(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Nome completo não pode ser nulo.");
    }
}
