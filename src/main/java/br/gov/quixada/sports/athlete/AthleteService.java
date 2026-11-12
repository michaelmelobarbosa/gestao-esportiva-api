package br.gov.quixada.sports.athlete;

import java.sql.SQLIntegrityConstraintViolationException;

import br.gov.quixada.sports.athlete.exception.AthleteNotDeletableException;
import br.gov.quixada.sports.athlete.exception.AthleteNotFoundException;
import br.gov.quixada.sports.athlete.exception.CpfAlreadyRegisteredException;
import br.gov.quixada.sports.common.error.ActiveReferenceException;
import br.gov.quixada.sports.shared.CpfUtils;
import br.gov.quixada.sports.shared.FullNameUtils;
import br.gov.quixada.sports.registration.AthleteRegistrationRepository;
import br.gov.quixada.sports.registration.RegistrationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AthleteService {

    private final AthleteRepository repository;
    private final AthleteRegistrationRepository athleteRegistrationRepository;


    @Transactional(readOnly = true)
    public Page<Athlete> findAll(String name, AthleteStatus status, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        }

        var normalizedName = FullNameUtils.normalize(name);
        return status == null
                ? repository.findByNormalizedFullNameStartingWith(normalizedName, pageable)
                : repository.findByNormalizedFullNameStartingWithAndStatus(normalizedName, status, pageable);
    }

    @Transactional(readOnly = true)
    public Athlete findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AthleteNotFoundException("Athlete not found"));
    }

    @Transactional(readOnly = true)
    public Athlete findByCpfOrThrowNotFound(String cpf) {
        return repository.findByCpf(CpfUtils.normalize(cpf))
                .orElseThrow(() -> new AthleteNotFoundException("Athlete not found"));
    }

    @Transactional
    public Athlete save(Athlete athlete) {
        athlete.setNormalizedCpf(athlete.getCpf());
        athlete.setNormalizedFullName(athlete.getFullName());
        athlete.setStatus(AthleteStatus.ACTIVE);

        try {
            return repository.save(athlete);
        } catch (DataIntegrityViolationException e) {
            // MySQL error 1062 = duplicate entry (unique CPF constraint)
            Throwable cause = e.getMostSpecificCause();
            if (cause instanceof SQLIntegrityConstraintViolationException sql && sql.getErrorCode() == 1062) {
                throw new CpfAlreadyRegisteredException("CPF already registered");
            }
            throw e;
        }
    }

    /**
     * Physical deletion of the athlete. Allowed only for an INACTIVE record with no
     * registrations that still occupy a slot; registrations that already free a slot
     * (CANCELED/REJECTED) are removed as well.
     */
    @Transactional
    public void hardDelete(Long id) {
        Athlete athlete = findByIdOrThrowNotFound(id);

        if (athlete.getStatus() != AthleteStatus.INACTIVE) {
            throw new AthleteNotDeletableException("An active athlete cannot be deleted; deactivate it first");
        }
        if (athleteRegistrationRepository.existsByAthleteIdAndStatusIn(id, RegistrationStatus.OCCUPY_SLOT)) {
            throw new AthleteNotDeletableException(
                    "Athlete has active registrations and cannot be deleted");
        }

        athleteRegistrationRepository.deleteByAthleteIdAndStatusIn(id, RegistrationStatus.FREE_SLOT);
        repository.delete(athlete);
    }

    @Transactional
    public Athlete activate(Long id) {
        Athlete athlete = findByIdOrThrowNotFound(id);
        athlete.activate();
        return athlete;
    }

    @Transactional
    public Athlete deactivate(Long id) {
        Athlete athlete = findByIdOrThrowNotFound(id);
        if (athleteRegistrationRepository.existsByAthleteIdAndStatusIn(id, RegistrationStatus.OCCUPY_SLOT)) {
            throw new ActiveReferenceException("Athlete has active registrations and cannot be deactivated");
        }
        athlete.deactivate();
        return athlete;
    }

    /**
     * CPF is immutable after creation; editing is blocked when INACTIVE (delegated to the entity).
     */
    @Transactional
    public Athlete update(Long id, Athlete athleteToUpdate) {
        Athlete existingAthlete = findByIdOrThrowNotFound(id);
        existingAthlete.updateDetails(
                athleteToUpdate.getFullName(),
                athleteToUpdate.getBirthDate(),
                athleteToUpdate.getAddress(),
                athleteToUpdate.getPhone(),
                athleteToUpdate.getGender()
        );
        return existingAthlete;
    }
}
