package br.gov.quixada.sports.match;

public enum MatchStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELED;

    public boolean canTransitionTo(MatchStatus target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case SCHEDULED -> target == IN_PROGRESS || target == CANCELED;
            case IN_PROGRESS -> target == COMPLETED || target == CANCELED;
            case COMPLETED -> false;
            case CANCELED -> false;
        };
    }
}
