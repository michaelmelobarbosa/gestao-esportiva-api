package br.gov.quixada.sports.registration;

import br.gov.quixada.sports.category.Category;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.team.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "db_team_registration")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TeamRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status;

    /** Free-form text: ad hoc communication, not tied to status. */
    @Column(length = 500)
    private String notes;

    /** Informational: set only on the transition to CANCELED. */
    private LocalDateTime canceledAt;

    /** Changes the lifecycle status; idempotent when unchanged. */
    public void changeStatus(RegistrationStatus newStatus, LocalDateTime now) {
        if (this.status == newStatus) return;
        this.status = newStatus;
        if (newStatus == RegistrationStatus.CANCELED) {
            this.canceledAt = now;
        }
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

    void setCompetition(Competition competition) {
        this.competition = competition;
    }

    void setCategory(Category category) {
        this.category = category;
    }

    void setTeam(Team team) {
        this.team = team;
    }

    void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = RegistrationStatus.PENDING;
        }
        this.notes = normalizeNotes(this.notes);
    }
}
