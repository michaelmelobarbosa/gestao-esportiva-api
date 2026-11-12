package br.gov.quixada.sports.category;

import br.gov.quixada.sports.category.exception.InvalidAgeException;
import br.gov.quixada.sports.category.exception.InactiveCategoryException;
import br.gov.quixada.sports.competition.Competition;
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
@Table(name = "db_category")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer minAge;

    @Column(nullable = false)
    private Integer maxAge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryStatus status;

    public void activate() {
        if (this.status == CategoryStatus.ACTIVE) return;
        this.status = CategoryStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status == CategoryStatus.INACTIVE) return;
        this.status = CategoryStatus.INACTIVE;
    }

    /** Updates mutable data. Competition is immutable after creation; blocked when INACTIVE. */
    public void updateDetails(String name, Integer minAge, Integer maxAge) {
        if (this.status == CategoryStatus.INACTIVE) {
            throw new InactiveCategoryException("Category is inactive; reactivate it before editing");
        }
        validateAges(minAge, maxAge);
        this.name = name;
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    public void validate() {
        validateAges(this.minAge, this.maxAge);
    }

    void setCompetition(Competition competition) {
        this.competition = competition;
    }

    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = CategoryStatus.ACTIVE;
        }
    }

    private void validateAges(Integer minAge, Integer maxAge) {
        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw new InvalidAgeException("Min age cannot be greater than max age");
        }
    }
}
