package br.gov.quixada.sports.modality;

import br.gov.quixada.sports.common.error.ActiveReferenceException;
import br.gov.quixada.sports.competition.CompetitionRepository;
import br.gov.quixada.sports.competition.CompetitionStatus;
import br.gov.quixada.sports.team.TeamRepository;
import br.gov.quixada.sports.team.TeamStatus;
import br.gov.quixada.sports.modality.exception.ModalityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModalityService {

    private final ModalityRepository repository;
    private final TeamRepository teamRepository;
    private final CompetitionRepository competitionRepository;

    @Transactional(readOnly = true)
    public Page<Modality> findAll(String name, ModalityStatus status, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        }
        return status == null
                ? repository.findByNameContainingIgnoreCase(name, pageable)
                : repository.findByNameContainingIgnoreCaseAndStatus(name, status, pageable);
    }

    @Transactional(readOnly = true)
    public Modality findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ModalityNotFoundException("Modality not found"));
    }

    @Transactional
    public Modality save(Modality modality) {
        modality.setStatus(ModalityStatus.ACTIVE);
        return repository.save(modality);
    }

    @Transactional
    public Modality activate(Long id) {
        Modality modality = findByIdOrThrowNotFound(id);
        modality.activate();
        return modality;
    }

    @Transactional
    public Modality deactivate(Long id) {
        Modality modality = findByIdOrThrowNotFound(id);
        boolean hasActiveTeam = teamRepository.existsByModalityIdAndStatus(id, TeamStatus.ACTIVE);
        boolean hasActiveCompetition = competitionRepository.existsByModalityIdAndStatusNotIn(
                id, List.of(CompetitionStatus.COMPLETED, CompetitionStatus.CANCELED));
        if (hasActiveTeam || hasActiveCompetition) {
            throw new ActiveReferenceException("Modality has active teams or competitions and cannot be deactivated");
        }
        modality.deactivate();
        return modality;
    }

    /** Updates mutable data only (name). Editing is blocked when INACTIVE. */
    @Transactional
    public Modality update(Long id, Modality modalityToUpdate) {
        Modality existingModality = findByIdOrThrowNotFound(id);
        existingModality.updateDetails(modalityToUpdate.getName());
        return existingModality;
    }
}
