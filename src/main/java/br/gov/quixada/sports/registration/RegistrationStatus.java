package br.gov.quixada.sports.registration;

import java.util.List;

public enum RegistrationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELED,
    INACTIVE;

    public static final List<RegistrationStatus> OCCUPY_SLOT = List.of(PENDING, APPROVED, INACTIVE);

    public static final List<RegistrationStatus> FREE_SLOT = List.of(CANCELED, REJECTED);

    public boolean occupiesSlot() {
        return OCCUPY_SLOT.contains(this);
    }

    public boolean freesSlot() {
        return FREE_SLOT.contains(this);
    }

    public boolean canTransitionTo(RegistrationStatus target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case PENDING -> target == APPROVED || target == REJECTED || target == CANCELED || target == INACTIVE;
            case APPROVED -> target == CANCELED || target == INACTIVE;
            case REJECTED -> target == PENDING || target == CANCELED;
            case INACTIVE -> target == PENDING;
            case CANCELED -> false;
        };
    }
}
