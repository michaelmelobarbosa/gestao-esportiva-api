package br.gov.quixada.sports.shared;

import java.util.Objects;

public final class CpfUtils {

    private CpfUtils() {
    }

    public static String normalize(String cpf) {
        return Objects.requireNonNull(cpf, "CPF cannot be null.")
                .replaceAll("\\D", "");
    }
}
