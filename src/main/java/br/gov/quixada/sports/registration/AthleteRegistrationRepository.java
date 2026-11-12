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
public interface AthleteRegistrationRepository extends JpaRepository<AthleteRegistration, Long> {

    @Override
    @EntityGraph(attributePaths = {"athlete", "teamRegistration"})
    Page<AthleteRegistration> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"athlete", "teamRegistration"})
    Page<AthleteRegistration> findByTeamRegistrationId(Long teamRegistrationId, Pageable pageable);

    @EntityGraph(attributePaths = {"athlete", "teamRegistration"})
    Page<AthleteRegistration> findByStatus(RegistrationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"athlete", "teamRegistration"})
    Page<AthleteRegistration> findByTeamRegistrationIdAndStatus(Long teamRegistrationId, RegistrationStatus status,
                                                           Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"athlete", "teamRegistration"})
    Optional<AthleteRegistration> findById(Long id);

    List<AthleteRegistration> findByTeamRegistrationId(Long teamRegistrationId);

    boolean existsByAthleteIdAndStatusIn(Long athleteId, Collection<RegistrationStatus> statuses);

    /**
     * Physically removes the athlete's registrations with the given statuses. Used by
     * hard delete, which only removes registrations that already free a slot (CANCELED/REJECTED).
     */
    long deleteByAthleteIdAndStatusIn(Long athleteId, Collection<RegistrationStatus> statuses);

    boolean existsByTeamRegistrationIdAndAthleteIdAndStatusIn(Long teamRegistrationId, Long athleteId,
                                                            Collection<RegistrationStatus> statuses);

    long countByTeamRegistrationIdAndStatusIn(Long teamRegistrationId, Collection<RegistrationStatus> statuses);

    boolean existsByTeamRegistrationIdAndJerseyNumberAndStatusIn(Long teamRegistrationId, String jerseyNumber,
                                                                Collection<RegistrationStatus> statuses);

    boolean existsByTeamRegistrationCompetitionIdAndAthleteIdAndStatusIn(Long competitionId, Long athleteId,
                                                                      Collection<RegistrationStatus> statuses);

    boolean existsByTeamRegistrationIdAndAthleteIdAndIdNotAndStatusIn(Long teamRegistrationId, Long athleteId, Long id,
                                                                   Collection<RegistrationStatus> statuses);

    boolean existsByTeamRegistrationIdAndJerseyNumberAndIdNotAndStatusIn(Long teamRegistrationId, String jerseyNumber,
                                                                       Long id, Collection<RegistrationStatus> statuses);

    boolean existsByTeamRegistrationCompetitionIdAndAthleteIdAndIdNotAndStatusIn(Long competitionId, Long athleteId,
                                                                             Long id,
                                                                             Collection<RegistrationStatus> statuses);
}
