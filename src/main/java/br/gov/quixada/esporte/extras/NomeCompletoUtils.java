package br.gov.quixada.esporte.extras;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

public final class NomeCompletoUtils {

    private NomeCompletoUtils() {
    }

    public static String normalize(String nomeCompleto) {

        String semAcento = Normalizer
                .normalize(Objects.requireNonNull(nomeCompleto, "Nome completo não pode ser nulo."),
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String somenteLetras = semAcento.replaceAll("[^A-Za-z]", "").toLowerCase(Locale.ROOT);

        return somenteLetras;
    }

}
