package br.gov.quixada.sports.competition;

import br.gov.quixada.sports.competition.exception.CompetitionNotEditableException;
import br.gov.quixada.sports.competition.exception.InvalidPeriodException;
import br.gov.quixada.sports.modality.Modality;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "db_competition")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "competition_year", nullable = false)
    private Integer year;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "modality_id", nullable = false)
    private Modality modality;

    @Column(nullable = false)
    private LocalDate registrationStartDate;

    @Column(nullable = false)
    private LocalDate registrationEndDate;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompetitionStatus status;

    /** Free-form text: ad hoc communication (cancellation reason, extension, etc.). */
    @Column(length = 500)
    private String notes;

    /** Informational: set only on the transition to CANCELED. */
    private LocalDateTime canceledAt;

    /** Manual cancellation (terminal). */
    public void cancelar(LocalDateTime now) {
        this.status = CompetitionStatus.CANCELED;
        this.canceledAt = now;
    }

    /**
     * Free-form field: can be set, changed, or cleared in any status.
     * Blank text is normalized to null.
     */
    public void updateNotes(String notes) {
        this.notes = normalizeNotes(notes);
    }

    private static String normalizeNotes(String notes) {
        return (notes == null || notes.isBlank()) ? null : notes.trim();
    }

    /** Recalculates the status based on the dates; CANCELED is terminal. */
    public void syncStatus(LocalDate hoje) {
        if (this.status == CompetitionStatus.CANCELED) {
            return;
        }
        this.status = calculateStatus(hoje);
    }

    public CompetitionStatus calculateStatus(LocalDate hoje) {
        if (this.status == CompetitionStatus.CANCELED) {
            return CompetitionStatus.CANCELED;
        }
        if (registrationStartDate == null || registrationEndDate == null || startDate == null || endDate == null) {
            return this.status != null ? this.status : CompetitionStatus.PLANNING;
        }
        if (hoje.isAfter(this.endDate)) {
            return CompetitionStatus.COMPLETED;
        }
        if (!hoje.isBefore(this.startDate)) {
            return CompetitionStatus.IN_PROGRESS;
        }
        if (hoje.isAfter(this.registrationEndDate)) {
            return CompetitionStatus.REGISTRATION_CLOSED;
        }
        if (!hoje.isBefore(this.registrationStartDate)) {
            return CompetitionStatus.REGISTRATION_OPEN;
        }
        return CompetitionStatus.PLANNING;
    }

    /**
     * Updates mutable data. Modality is immutable after creation;
     * editing is blocked when COMPLETED or CANCELED.
     */
    public void updateDetails(String name, Integer year, LocalDate registrationStartDate,
                               LocalDate registrationEndDate, LocalDate startDate, LocalDate endDate) {
        if (this.status == CompetitionStatus.COMPLETED || this.status == CompetitionStatus.CANCELED) {
            throw new CompetitionNotEditableException("A completed or canceled competition cannot be edited");
        }
        validatePeriod(registrationStartDate, registrationEndDate);
        validatePeriod(startDate, endDate);
        this.name = name;
        this.year = year;
        this.registrationStartDate = registrationStartDate;
        this.registrationEndDate = registrationEndDate;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void validate() {
        validatePeriod(this.registrationStartDate, this.registrationEndDate);
        validatePeriod(this.startDate, this.endDate);
    }

    void setModality(Modality modality) {
        this.modality = modality;
    }

    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = CompetitionStatus.PLANNING;
        }
        this.notes = normalizeNotes(this.notes);
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && !endDate.isAfter(startDate)) {
            throw new InvalidPeriodException("End date must be after the start date");
        }
    }
}
