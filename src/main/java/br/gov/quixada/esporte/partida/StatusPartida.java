package br.gov.quixada.esporte.partida;

public enum StatusPartida {
    AGENDADA,
    EM_ANDAMENTO,
    FINALIZADA,
    CANCELADA;

    public boolean podeTransicionarPara(StatusPartida novo) {
        if (this == novo) {
            return true;
        }
        return switch (this) {
            case AGENDADA -> novo == EM_ANDAMENTO || novo == CANCELADA;
            case EM_ANDAMENTO -> novo == FINALIZADA || novo == CANCELADA;
            case FINALIZADA -> false;
            case CANCELADA -> false;
        };
    }
}
