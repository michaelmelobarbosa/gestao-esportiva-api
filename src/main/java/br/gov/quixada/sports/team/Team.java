package br.gov.quixada.sports.team;

import br.gov.quixada.sports.club.Club;
import br.gov.quixada.sports.team.exception.InactiveTeamException;
import br.gov.quixada.sports.modality.Modality;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "db_team")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modality_id", nullable = false)
    private Modality modality;

    @Column(nullable = false, length = 100)
    private String manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamStatus status;

    /** Updates mutable data. Club and modality are immutable after creation; blocked when INACTIVE. */
    public void updateDetails(String name, String manager) {
        if (this.status == TeamStatus.INACTIVE) {
            throw new InactiveTeamException("Team is inactive; reactivate it before editing");
        }
        this.name = name;
        this.manager = manager;
    }

    public void activate() {
        if (this.status == TeamStatus.ACTIVE) return;
        this.status = TeamStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status == TeamStatus.INACTIVE) return;
        this.status = TeamStatus.INACTIVE;
    }

    void setClub(Club club) {
        this.club = club;
    }

    void setModality(Modality modality) {
        this.modality = modality;
    }

    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = TeamStatus.ACTIVE;
        }
    }
}
