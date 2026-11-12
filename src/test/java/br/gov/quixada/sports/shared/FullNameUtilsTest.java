package br.gov.quixada.sports.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FullNameUtils")
class FullNameUtilsTest {

    @Test
    @DisplayName("should remove accents, case and spaces")
    void shouldRemoveAccentCaseAndSpaces() {
        assertThat(FullNameUtils.normalize("João Silva")).isEqualTo("joaosilva");
    }

    @Test
    @DisplayName("should normalize cedilla")
    void shouldNormalizeCedilla() {
        assertThat(FullNameUtils.normalize("Conceição")).isEqualTo("conceicao");
    }

    @Test
    @DisplayName("should return empty when there are only symbols and spaces")
    void shouldReturnEmptyWhenOnlySymbolsAndSpaces() {
        assertThat(FullNameUtils.normalize("@#$ %")).isEmpty();
    }

    @Test
    @DisplayName("should be idempotent for already-normalized input")
    void shouldBeIdempotentForNormalizedInput() {
        assertThat(FullNameUtils.normalize("joaosilva")).isEqualTo("joaosilva");
    }

    @Test
    @DisplayName("should throw NullPointerException when the name is null")
    void shouldThrowNpeWhenNameIsNull() {
        assertThatThrownBy(() -> FullNameUtils.normalize(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Full name cannot be null.");
    }
}
