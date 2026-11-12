package br.gov.quixada.sports.shared;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

public final class FullNameUtils {

    private FullNameUtils() {
    }

    public static String normalize(String fullName) {

        String semAcento = Normalizer
                .normalize(Objects.requireNonNull(fullName, "Full name cannot be null."),
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String somenteLetras = semAcento.replaceAll("[^A-Za-z]", "").toLowerCase(Locale.ROOT);

        return somenteLetras;
    }

}
