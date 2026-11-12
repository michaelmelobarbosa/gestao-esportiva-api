package br.gov.quixada.sports.match;

import br.gov.quixada.sports.registration.TeamRegistration;
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

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "db_match")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false)
    private Integer round;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Column(length = 150)
    private String venue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status;

    private Integer homeGoals;

    private Integer awayGoals;

    /** Free-form text: ad hoc communication, not tied to status. */
    @Column(length = 500)
    private String notes;

    /** Informational: set only on the transition to CANCELED. */
    private LocalDateTime canceledAt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "home_registration_id", nullable = false)
    private TeamRegistration homeRegistration;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "away_registration_id", nullable = false)
    private TeamRegistration awayRegistration;

    public void setRegistrations(TeamRegistration home, TeamRegistration away) {
        this.homeRegistration = home;
        this.awayRegistration = away;
    }

    public void updateDetails(Integer round, LocalDateTime scheduledAt, String venue,
                               TeamRegistration home, TeamRegistration away) {
        this.round = round;
        this.scheduledAt = scheduledAt;
        this.venue = venue;
        this.homeRegistration = home;
        this.awayRegistration = away;
    }

    public void changeStatus(MatchStatus newStatus, LocalDateTime now) {
        if (this.status == newStatus) {
            return;
        }
        this.status = newStatus;
        if (newStatus == MatchStatus.CANCELED) {
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

    public void recordResult(Integer homeGoals, Integer awayGoals) {
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
        this.status = MatchStatus.COMPLETED;
    }

    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = MatchStatus.SCHEDULED;
        }
        this.notes = normalizeNotes(this.notes);
    }
}
