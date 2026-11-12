package br.gov.quixada.sports.match;

public enum MatchStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELED;

    public boolean canTransitionTo(MatchStatus novo) {
        if (this == novo) {
            return true;
        }
        return switch (this) {
            case SCHEDULED -> novo == IN_PROGRESS || novo == CANCELED;
            case IN_PROGRESS -> novo == COMPLETED || novo == CANCELED;
            case COMPLETED -> false;
            case CANCELED -> false;
        };
    }
}
