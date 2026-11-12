package br.gov.quixada.sports.registration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRegistrationRepository extends JpaRepository<TeamRegistration, Long> {

    @Override
    @EntityGraph(attributePaths = {"competition", "category", "team"})
    Page<TeamRegistration> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"competition", "category", "team"})
    Page<TeamRegistration> findByCompetitionId(Long competitionId, Pageable pageable);

    List<TeamRegistration> findByCompetitionId(Long competitionId);

    boolean existsByCompetitionIdAndTeamIdAndStatusIn(Long competitionId, Long teamId,
                                                       Collection<RegistrationStatus> statuses);

    boolean existsByCompetitionIdAndTeamIdAndStatusInAndIdNot(Long competitionId, Long teamId,
                                                               Collection<RegistrationStatus> statuses, Long id);

    @EntityGraph(attributePaths = {"competition", "category", "team"})
    List<TeamRegistration> findByCompetitionIdAndCategoryIdAndStatus(Long competitionId, Long categoryId,
                                                                    RegistrationStatus status);

    boolean existsByTeamIdAndStatusIn(Long teamId, Collection<RegistrationStatus> statuses);

    boolean existsByCategoryIdAndStatusIn(Long categoryId, Collection<RegistrationStatus> statuses);

    @EntityGraph(attributePaths = {"competition", "category", "team"})
    Page<TeamRegistration> findByStatus(RegistrationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"competition", "category", "team"})
    Page<TeamRegistration> findByCompetitionIdAndStatus(Long competitionId, RegistrationStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"competition", "category", "team"})
    Optional<TeamRegistration> findById(Long id);
}
