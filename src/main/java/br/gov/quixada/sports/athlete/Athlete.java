package br.gov.quixada.sports.athlete;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.gov.quixada.sports.shared.CpfUtils;
import br.gov.quixada.sports.shared.Address;
import br.gov.quixada.sports.shared.FullNameUtils;
import br.gov.quixada.sports.shared.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import br.gov.quixada.sports.athlete.exception.InactiveAthleteException;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "db_athletes")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Athlete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(unique = true, nullable = false, length = 11)
    @Pattern(regexp = "\\d{11}", message = "CPF must contain only digits and have 11 characters")
    private String cpf;

    @Column(nullable = false)
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @Embedded
    @Valid
    @NotNull
    private Address address;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AthleteStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "normalized_full_name", nullable = false, length = 150)
    private String normalizedFullName;


    public void activate() {
        if (this.status == AthleteStatus.ACTIVE) return;
        this.status = AthleteStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status == AthleteStatus.INACTIVE) return;
        this.status = AthleteStatus.INACTIVE;
    }

    /**
     * Updates mutable data. CPF is immutable after creation; blocked when INACTIVE.
     */
    public void updateDetails(String fullName, LocalDate birthDate, Address address, String phone, Gender gender) {
        if (this.status == AthleteStatus.INACTIVE) {
            throw new InactiveAthleteException("Athlete is inactive; reactivate it before editing");
        }
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.address = address;
        this.phone = phone;
        this.gender = gender;
    }

    void setNormalizedCpf(String cpf) {
        this.cpf = CpfUtils.normalize(cpf);
    }

    void setNormalizedFullName(String fullName) {
        this.normalizedFullName = FullNameUtils.normalize(fullName);
    }

    void setStatus(AthleteStatus status) {
        this.status = status;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.cpf != null) {
            this.cpf = CpfUtils.normalize(this.cpf);
        }
        if (this.status == null) {
            this.status = AthleteStatus.ACTIVE;
        }
        if (this.fullName != null) {
            this.normalizedFullName = FullNameUtils.normalize(this.fullName);
        }
    }

    @PreUpdate
    private void preUpdate() {
        if (this.cpf != null) {
            this.cpf = CpfUtils.normalize(this.cpf);
        }
        if (this.fullName != null) {
            this.normalizedFullName = FullNameUtils.normalize(this.fullName);
        }
    }

}
