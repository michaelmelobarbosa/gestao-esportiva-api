package br.gov.quixada.esporte.inscricao;

import java.util.List;

public enum StatusInscricao {
    PENDENTE,
    APROVADA,
    REJEITADA,
    CANCELADA,
    INATIVA;

    public static final List<StatusInscricao> OCUPAM_VAGA = List.of(PENDENTE, APROVADA, INATIVA);

    public static final List<StatusInscricao> LIBERAM_VAGA = List.of(CANCELADA, REJEITADA);

    public boolean ocupaVaga() {
        return OCUPAM_VAGA.contains(this);
    }

    public boolean liberaVaga() {
        return LIBERAM_VAGA.contains(this);
    }

    public boolean podeTransicionarPara(StatusInscricao novo) {
        if (this == novo) {
            return true;
        }
        return switch (this) {
            case PENDENTE -> novo == APROVADA || novo == REJEITADA || novo == CANCELADA || novo == INATIVA;
            case APROVADA -> novo == CANCELADA || novo == INATIVA;
            case REJEITADA -> novo == PENDENTE || novo == CANCELADA;
            case INATIVA -> novo == PENDENTE;
            case CANCELADA -> false;
        };
    }
}
