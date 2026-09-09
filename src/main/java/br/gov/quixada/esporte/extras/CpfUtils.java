package br.gov.quixada.esporte.extras;

public final class CpfUtils {
    public static String normalize(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }
}
