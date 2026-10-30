package br.gov.quixada.esporte.extras;

import java.util.Objects;

public final class CpfUtils {

    private CpfUtils() {
    }

    public static String normalize(String cpf) {
        return Objects.requireNonNull(cpf, "CPF não pode ser nulo.")
                .replaceAll("\\D", "");
    }
}
