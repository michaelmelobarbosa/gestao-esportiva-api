package br.gov.quixada.sports.registration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RegistrationStatus")
class RegistrationStatusTest {

    @Test
    @DisplayName("should indicate which statuses occupy and which free a slot")
    void shouldIndicateSlotOccupancy() {
        assertThat(RegistrationStatus.PENDING.occupiesSlot()).isTrue();
        assertThat(RegistrationStatus.APPROVED.occupiesSlot()).isTrue();
        assertThat(RegistrationStatus.INACTIVE.occupiesSlot()).isTrue();
        assertThat(RegistrationStatus.CANCELED.freesSlot()).isTrue();
        assertThat(RegistrationStatus.REJECTED.freesSlot()).isTrue();
    }

    @Test
    @DisplayName("should allow valid transitions")
    void shouldAllowValidTransitions() {
        assertThat(RegistrationStatus.PENDING.canTransitionTo(RegistrationStatus.APPROVED)).isTrue();
        assertThat(RegistrationStatus.PENDING.canTransitionTo(RegistrationStatus.REJECTED)).isTrue();
        assertThat(RegistrationStatus.APPROVED.canTransitionTo(RegistrationStatus.CANCELED)).isTrue();
        assertThat(RegistrationStatus.INACTIVE.canTransitionTo(RegistrationStatus.PENDING)).isTrue();
        assertThat(RegistrationStatus.REJECTED.canTransitionTo(RegistrationStatus.PENDING)).isTrue();
    }

    @Test
    @DisplayName("should block invalid transitions")
    void shouldBlockInvalidTransitions() {
        assertThat(RegistrationStatus.CANCELED.canTransitionTo(RegistrationStatus.PENDING)).isFalse();
        assertThat(RegistrationStatus.APPROVED.canTransitionTo(RegistrationStatus.PENDING)).isFalse();
        assertThat(RegistrationStatus.REJECTED.canTransitionTo(RegistrationStatus.APPROVED)).isFalse();
    }
}
