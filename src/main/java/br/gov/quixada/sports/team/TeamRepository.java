package br.gov.quixada.sports.team;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    @Override
    @EntityGraph(attributePaths = {"club", "modality"})
    Page<Team> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"club", "modality"})
    Page<Team> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @EntityGraph(attributePaths = {"club", "modality"})
    Page<Team> findByStatus(TeamStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"club", "modality"})
    Page<Team> findByNameContainingIgnoreCaseAndStatus(String name, TeamStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"club", "modality"})
    Optional<Team> findById(Long id);

    boolean existsByClubIdAndStatus(Long clubId, TeamStatus status);

    boolean existsByModalityIdAndStatus(Long modalityId, TeamStatus status);
}
