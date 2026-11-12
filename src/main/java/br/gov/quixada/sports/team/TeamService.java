package br.gov.quixada.sports.team;

import br.gov.quixada.sports.club.Club;
import br.gov.quixada.sports.club.ClubRepository;
import br.gov.quixada.sports.club.ClubStatus;
import br.gov.quixada.sports.club.exception.InactiveClubException;
import br.gov.quixada.sports.club.exception.ClubNotFoundException;
import br.gov.quixada.sports.common.error.ActiveReferenceException;
import br.gov.quixada.sports.team.exception.TeamNotFoundException;
import br.gov.quixada.sports.registration.TeamRegistrationRepository;
import br.gov.quixada.sports.registration.RegistrationStatus;
import br.gov.quixada.sports.modality.ModalityStatus;
import br.gov.quixada.sports.modality.exception.InactiveModalityException;
import br.gov.quixada.sports.modality.Modality;
import br.gov.quixada.sports.modality.ModalityRepository;
import br.gov.quixada.sports.modality.exception.ModalityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository repository;
    private final ClubRepository clubRepository;
    private final ModalityRepository modalityRepository;
    private final TeamRegistrationRepository teamRegistrationRepository;

    @Transactional(readOnly = true)
    public Page<Team> findAll(String name, TeamStatus status, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        }
        return status == null
                ? repository.findByNameContainingIgnoreCase(name, pageable)
                : repository.findByNameContainingIgnoreCaseAndStatus(name, status, pageable);
    }

    @Transactional(readOnly = true)
    public Team findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException("Team not found"));
    }

    @Transactional
    public Team save(Team team, Long clubId, Long modalityId) {
        Club club = findClub(clubId);
        if (club.getStatus() != ClubStatus.ACTIVE) {
            throw new InactiveClubException("Club is inactive and cannot be linked to a team");
        }

        var modality = findModality(modalityId);
        if (modality.getStatus() != ModalityStatus.ACTIVE) {
            throw new InactiveModalityException("Modality is inactive and cannot be linked to a team");
        }

        team.activate();
        team.setClub(club);
        team.setModality(modality);
        return repository.save(team);
    }

    @Transactional
    public Team activate(Long id) {
        Team team = findByIdOrThrowNotFound(id);
        team.activate();
        return team;
    }

    @Transactional
    public Team deactivate(Long id) {
        Team team = findByIdOrThrowNotFound(id);
        if (teamRegistrationRepository.existsByTeamIdAndStatusIn(id, RegistrationStatus.OCCUPY_SLOT)) {
            throw new ActiveReferenceException("Team has active registrations and cannot be deactivated");
        }
        team.deactivate();
        return team;
    }

    /**
     * Updates mutable data (name and manager). Club and modality are immutable after creation.
     */
    @Transactional
    public Team update(Long id, Team teamToUpdate) {
        Team existingTeam = findByIdOrThrowNotFound(id);
        existingTeam.updateDetails(
                teamToUpdate.getName(),
                teamToUpdate.getManager()
        );
        return existingTeam;
    }

    private Club findClub(Long id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new ClubNotFoundException("Club not found"));
    }

    private Modality findModality(Long id) {
        return modalityRepository.findById(id)
                .orElseThrow(() -> new ModalityNotFoundException("Modality not found"));
    }
}
