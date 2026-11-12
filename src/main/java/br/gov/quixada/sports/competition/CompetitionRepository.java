package br.gov.quixada.sports.competition;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    @Override
    @EntityGraph(attributePaths = "modality")
    Page<Competition> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "modality")
    Page<Competition> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @EntityGraph(attributePaths = "modality")
    Page<Competition> findByStatus(CompetitionStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "modality")
    Page<Competition> findByNameContainingIgnoreCaseAndStatus(String name, CompetitionStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "modality")
    Optional<Competition> findById(Long id);

    boolean existsByModalityIdAndStatusNotIn(Long modalityId, Collection<CompetitionStatus> statuses);
}
