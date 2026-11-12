package br.gov.quixada.sports.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Override
    @EntityGraph(attributePaths = "competition")
    Page<Category> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "competition")
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @EntityGraph(attributePaths = "competition")
    Page<Category> findByStatus(CategoryStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "competition")
    Page<Category> findByNameContainingIgnoreCaseAndStatus(String name, CategoryStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "competition")
    Optional<Category> findById(Long id);

    boolean existsByCompetitionIdAndMinAgeAndMaxAgeAndStatus(Long competitionId, Integer minAge,
                                                                      Integer maxAge, CategoryStatus status);

    boolean existsByCompetitionIdAndMinAgeAndMaxAgeAndStatusAndIdNot(Long competitionId, Integer minAge,
                                                                             Integer maxAge, CategoryStatus status,
                                                                             Long id);
}
