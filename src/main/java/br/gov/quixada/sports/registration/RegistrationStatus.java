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

    public boolean canTransitionTo(RegistrationStatus novo) {
        if (this == novo) {
            return true;
        }
        return switch (this) {
            case PENDING -> novo == APPROVED || novo == REJECTED || novo == CANCELED || novo == INACTIVE;
            case APPROVED -> novo == CANCELED || novo == INACTIVE;
            case REJECTED -> novo == PENDING || novo == CANCELED;
            case INACTIVE -> novo == PENDING;
            case CANCELED -> false;
        };
    }
}
