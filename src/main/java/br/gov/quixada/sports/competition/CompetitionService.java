package br.gov.quixada.sports.competition;

import br.gov.quixada.sports.competition.exception.CompetitionNotFoundException;
import br.gov.quixada.sports.competition.exception.CompetitionStatusTransitionException;
import br.gov.quixada.sports.registration.TeamRegistrationService;
import br.gov.quixada.sports.modality.Modality;
import br.gov.quixada.sports.match.MatchService;
import br.gov.quixada.sports.modality.ModalityRepository;
import br.gov.quixada.sports.modality.ModalityStatus;
import br.gov.quixada.sports.modality.exception.InactiveModalityException;
import br.gov.quixada.sports.modality.exception.ModalityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CompetitionService {

    private final CompetitionRepository repository;
    private final ModalityRepository modalityRepository;
    private final TeamRegistrationService teamRegistrationService;
    private final MatchService matchService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<Competition> findAll(String name, CompetitionStatus status, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        }
        return status == null
                ? repository.findByNameContainingIgnoreCase(name, pageable)
                : repository.findByNameContainingIgnoreCaseAndStatus(name, status, pageable);
    }

    @Transactional(readOnly = true)
    public Competition findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new CompetitionNotFoundException("Competition not found"));
    }

    @Transactional
    public Competition save(Competition competition, Long modalityId) {
        competition.validate();
        competition.setModality(findModality(modalityId));
        competition.syncStatus(LocalDate.now(clock));
        return repository.save(competition);
    }

    @Transactional
    public Competition changeStatus(Long id, CompetitionStatus status) {
        if (status != CompetitionStatus.CANCELED) {
            throw new CompetitionStatusTransitionException(
                    "Competition status is updated automatically based on dates; only CANCELED is manual");
        }

        Competition competition = findByIdOrThrowNotFound(id);
        if (competition.getStatus() == CompetitionStatus.CANCELED) {
            return competition;
        }

        competition.cancelar(LocalDateTime.now(clock));
        teamRegistrationService.cancelByCompetition(id);
        matchService.cancelByCompetition(id);
        return competition;
    }

    /**
     * Free-form communication field: can be set, changed, or cleared in any
     * status (including CANCELED/COMPLETED), without business checks.
     */
    @Transactional
    public Competition updateNotes(Long id, String notes) {
        Competition competition = findByIdOrThrowNotFound(id);
        competition.updateNotes(notes);
        return competition;
    }

    /**
     * Updates mutable data (name, year, and dates). Modality is immutable after creation;
     * blocked when COMPLETED or CANCELED.
     */
    @Transactional
    public Competition update(Long id, Competition competitionToUpdate) {
        Competition existingCompetition = findByIdOrThrowNotFound(id);
        existingCompetition.updateDetails(
                competitionToUpdate.getName(),
                competitionToUpdate.getYear(),
                competitionToUpdate.getRegistrationStartDate(),
                competitionToUpdate.getRegistrationEndDate(),
                competitionToUpdate.getStartDate(),
                competitionToUpdate.getEndDate()
        );
        existingCompetition.syncStatus(LocalDate.now(clock));
        return existingCompetition;
    }

    private Modality findModality(Long id) {
        Modality modality = modalityRepository.findById(id)
                .orElseThrow(() -> new ModalityNotFoundException("Modality not found"));
        if (modality.getStatus() != ModalityStatus.ACTIVE) {
            throw new InactiveModalityException("Modality is inactive and cannot be linked to a competition");
        }
        return modality;
    }
}
