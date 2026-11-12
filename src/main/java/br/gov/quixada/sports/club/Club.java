package br.gov.quixada.sports.club;

import br.gov.quixada.sports.club.exception.InactiveClubException;
import br.gov.quixada.sports.shared.Address;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "db_club")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String manager;

    @Column(nullable = false, length = 20)
    private String phone;

    @Embedded
    @Valid
    @NotNull
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClubStatus status;

    /** Updates mutable data; blocked when INACTIVE. */
    public void updateDetails(String name, String manager, String phone, Address address) {
        if (this.status == ClubStatus.INACTIVE) {
            throw new InactiveClubException("Club is inactive; reactivate it before editing");
        }
        this.name = name;
        this.manager = manager;
        this.phone = phone;
        this.address = address;
    }

    public void activate() {
        if (this.status == ClubStatus.ACTIVE) return;
        this.status = ClubStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status == ClubStatus.INACTIVE) return;
        this.status = ClubStatus.INACTIVE;
    }

    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = ClubStatus.ACTIVE;
        }
    }
}
