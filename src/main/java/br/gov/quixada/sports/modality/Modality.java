package br.gov.quixada.sports.modality;

import br.gov.quixada.sports.modality.exception.InactiveModalityException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
@Table(name = "db_modality")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Modality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModalityStatus status;

    public void activate() {
        if (this.status == ModalityStatus.ACTIVE) return;
        this.status = ModalityStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status == ModalityStatus.INACTIVE) return;
        this.status = ModalityStatus.INACTIVE;
    }

    /** Updates mutable data. Editing is blocked when INACTIVE. */
    public void updateDetails(String name) {
        if (this.status == ModalityStatus.INACTIVE) {
            throw new InactiveModalityException("Modality is inactive; reactivate it before editing");
        }
        this.name = name;
    }

    void setStatus(ModalityStatus status) {
        this.status = status;
    }

    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = ModalityStatus.ACTIVE;
        }
    }
}
