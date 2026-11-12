package br.gov.quixada.sports.club;

import br.gov.quixada.sports.club.exception.ClubNotFoundException;
import br.gov.quixada.sports.common.error.ActiveReferenceException;
import br.gov.quixada.sports.team.TeamRepository;
import br.gov.quixada.sports.team.TeamStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository repository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public Page<Club> findAll(String name, ClubStatus status, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        }
        return status == null
                ? repository.findByNameContainingIgnoreCase(name, pageable)
                : repository.findByNameContainingIgnoreCaseAndStatus(name, status, pageable);
    }

    @Transactional(readOnly = true)
    public Club findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ClubNotFoundException("Club not found"));
    }

    @Transactional
    public Club save(Club club) {
        club.activate();
        return repository.save(club);
    }

    @Transactional
    public Club activate(Long id) {
        Club club = findByIdOrThrowNotFound(id);
        club.activate();
        return club;
    }

    @Transactional
    public Club deactivate(Long id) {
        Club club = findByIdOrThrowNotFound(id);
        if (teamRepository.existsByClubIdAndStatus(id, TeamStatus.ACTIVE)) {
            throw new ActiveReferenceException("Club has active teams and cannot be deactivated");
        }
        club.deactivate();
        return club;
    }

    /**
     * Updates mutable data. Status (ACTIVE/INACTIVE) changes only through soft delete.
     */
    @Transactional
    public Club update(Long id, Club clubToUpdate) {
        Club existingClub = findByIdOrThrowNotFound(id);
        existingClub.updateDetails(
                clubToUpdate.getName(),
                clubToUpdate.getManager(),
                clubToUpdate.getPhone(),
                clubToUpdate.getAddress()
        );
        return existingClub;
    }
}
