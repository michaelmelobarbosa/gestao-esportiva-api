package br.gov.quixada.sports.registration;

import br.gov.quixada.sports.athlete.Athlete;
import br.gov.quixada.sports.athlete.AthleteRepository;
import br.gov.quixada.sports.athlete.AthleteStatus;
import br.gov.quixada.sports.athlete.exception.AthleteNotFoundException;
import br.gov.quixada.sports.category.Category;
import br.gov.quixada.sports.category.CategoryStatus;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionStatus;
import br.gov.quixada.sports.registration.exception.AthleteOutsideCategoryException;
import br.gov.quixada.sports.registration.exception.AthleteAlreadyRegisteredException;
import br.gov.quixada.sports.registration.exception.AthleteRegistrationNotFoundException;
import br.gov.quixada.sports.registration.exception.TeamRegistrationNotFoundException;
import br.gov.quixada.sports.registration.exception.InvalidRegistrationException;
import br.gov.quixada.sports.registration.exception.JerseyNumberAlreadyUsedException;
import br.gov.quixada.sports.registration.exception.InvalidStatusTransitionException;
import br.gov.quixada.sports.registration.exception.LastActiveAthleteException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AthleteRegistrationService {

    private final AthleteRegistrationRepository repository;
    private final TeamRegistrationRepository teamRegistrationRepository;
    private final AthleteRepository athleteRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<AthleteRegistration> findAll(Long teamRegistrationId, RegistrationStatus status, Pageable pageable) {
        if (teamRegistrationId == null) {
            return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        }
        return status == null
                ? repository.findByTeamRegistrationId(teamRegistrationId, pageable)
                : repository.findByTeamRegistrationIdAndStatus(teamRegistrationId, status, pageable);
    }

    @Transactional(readOnly = true)
    public AthleteRegistration findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AthleteRegistrationNotFoundException("Athlete registration not found"));
    }

    @Transactional
    public AthleteRegistration save(AthleteRegistration registration, Long teamRegistrationId, Long athleteId) {
        TeamRegistration teamRegistration = findTeamRegistration(teamRegistrationId);
        Athlete athlete = findAthlete(athleteId);

        validateOpenCompetition(teamRegistration.getCompetition());
        validateActiveTeamRegistration(teamRegistration);
        validateActiveAthlete(athlete);
        validateActiveCategory(teamRegistration.getCategory());
        validateSlots(teamRegistrationId, teamRegistration.getCompetition().getId(), athleteId,
                registration.getJerseyNumber(), null);
        validateAge(athlete, teamRegistration.getCategory());

        registration.setTeamRegistration(teamRegistration);
        registration.setAthlete(athlete);
        registration.setStatus(RegistrationStatus.PENDING);
        return repository.save(registration);
    }

    @Transactional
    public AthleteRegistration changeStatus(Long id, RegistrationStatus status) {
        AthleteRegistration registration = findByIdOrThrowNotFound(id);

        if (!registration.getStatus().canTransitionTo(status)) {
            throw new InvalidStatusTransitionException(
                    "Invalid status transition: " + registration.getStatus() + " -> " + status);
        }

        validateLastAthleteRemoval(registration, status);

        if (status == RegistrationStatus.PENDING) {
            revalidateReactivation(registration);
        }

        registration.changeStatus(status, LocalDateTime.now(clock));
        return registration;
    }

    /**
     * Free-form communication field: can be set, changed, or cleared in any
     * status, without business checks.
     */
    @Transactional
    public AthleteRegistration updateNotes(Long id, String notes) {
        AthleteRegistration registration = findByIdOrThrowNotFound(id);
        registration.updateNotes(notes);
        return registration;
    }

    @Transactional
    public AthleteRegistration update(Long id, String jerseyNumber) {
        AthleteRegistration registration = findByIdOrThrowNotFound(id);

        if (jerseyNumber.equals(registration.getJerseyNumber())) {
            return registration;
        }

        Long teamRegistrationId = registration.getTeamRegistration().getId();
        if (repository.existsByTeamRegistrationIdAndJerseyNumberAndIdNotAndStatusIn(
                teamRegistrationId, jerseyNumber, id, RegistrationStatus.OCCUPY_SLOT)) {
            throw new JerseyNumberAlreadyUsedException("Jersey number already used in this registration");
        }

        registration.updateDetails(jerseyNumber);
        return registration;
    }

    private void revalidateReactivation(AthleteRegistration registration) {
        var teamRegistration = registration.getTeamRegistration();
        validateOpenCompetition(teamRegistration.getCompetition());
        validateActiveTeamRegistration(teamRegistration);
        validateActiveAthlete(registration.getAthlete());
        validateActiveCategory(teamRegistration.getCategory());
        validateSlots(teamRegistration.getId(), teamRegistration.getCompetition().getId(),
                registration.getAthlete().getId(), registration.getJerseyNumber(), registration.getId());
        validateAge(registration.getAthlete(), teamRegistration.getCategory());
    }

    private void validateSlots(Long teamRegistrationId, Long competitionId, Long athleteId,
                              String jerseyNumber, Long ignoredId) {
        boolean duplicateAthlete = ignoredId == null
                ? repository.existsByTeamRegistrationIdAndAthleteIdAndStatusIn(
                        teamRegistrationId, athleteId, RegistrationStatus.OCCUPY_SLOT)
                : repository.existsByTeamRegistrationIdAndAthleteIdAndIdNotAndStatusIn(
                        teamRegistrationId, athleteId, ignoredId, RegistrationStatus.OCCUPY_SLOT);
        if (duplicateAthlete) {
            throw new AthleteAlreadyRegisteredException("Athlete is already registered on this team");
        }

        boolean athleteOnAnotherTeam = ignoredId == null
                ? repository.existsByTeamRegistrationCompetitionIdAndAthleteIdAndStatusIn(
                        competitionId, athleteId, RegistrationStatus.OCCUPY_SLOT)
                : repository.existsByTeamRegistrationCompetitionIdAndAthleteIdAndIdNotAndStatusIn(
                        competitionId, athleteId, ignoredId, RegistrationStatus.OCCUPY_SLOT);
        if (athleteOnAnotherTeam) {
            throw new AthleteAlreadyRegisteredException("Athlete is already registered on another team in this competition");
        }

        boolean duplicateJersey = ignoredId == null
                ? repository.existsByTeamRegistrationIdAndJerseyNumberAndStatusIn(
                        teamRegistrationId, jerseyNumber, RegistrationStatus.OCCUPY_SLOT)
                : repository.existsByTeamRegistrationIdAndJerseyNumberAndIdNotAndStatusIn(
                        teamRegistrationId, jerseyNumber, ignoredId, RegistrationStatus.OCCUPY_SLOT);
        if (duplicateJersey) {
            throw new JerseyNumberAlreadyUsedException("Jersey number already used in this registration");
        }
    }

    private void validateLastAthleteRemoval(AthleteRegistration registration, RegistrationStatus newStatus) {
        boolean removeDaActive = newStatus == RegistrationStatus.CANCELED
                || newStatus == RegistrationStatus.INACTIVE
                || newStatus == RegistrationStatus.REJECTED;
        boolean estavaActive = registration.getStatus() == RegistrationStatus.PENDING
                || registration.getStatus() == RegistrationStatus.APPROVED;

        if (!removeDaActive || !estavaActive) {
            return;
        }

        var teamRegistration = registration.getTeamRegistration();
        if (teamRegistration == null || teamRegistration.getStatus() != RegistrationStatus.APPROVED) {
            return;
        }

        long activeAthletes = repository.countByTeamRegistrationIdAndStatusIn(
                teamRegistration.getId(), List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED));
        if (activeAthletes <= 1) {
            throw new LastActiveAthleteException(
                    "An approved team must keep at least one active athlete");
        }
    }

    private void validateActiveAthlete(Athlete athlete) {
        if (athlete.getStatus() != AthleteStatus.ACTIVE) {
            throw new InvalidRegistrationException("An inactive athlete cannot be registered");
        }
    }

    private void validateActiveCategory(Category category) {
        if (category.getStatus() != CategoryStatus.ACTIVE) {
            throw new InvalidRegistrationException("An inactive category cannot receive registrations");
        }
    }

    private void validateOpenCompetition(Competition competition) {
        if (competition.calculateStatus(LocalDate.now(clock)) != CompetitionStatus.REGISTRATION_OPEN) {
            throw new InvalidRegistrationException("Competition registration is not open");
        }
    }

    private void validateActiveTeamRegistration(TeamRegistration teamRegistration) {
        if (teamRegistration.getStatus() != RegistrationStatus.PENDING
                && teamRegistration.getStatus() != RegistrationStatus.APPROVED) {
            throw new InvalidRegistrationException("Team registration is not active");
        }
    }

    private void validateAge(Athlete athlete, Category category) {
        if (athlete.getBirthDate() == null) return;
        int age = Period.between(athlete.getBirthDate(), LocalDate.now(clock)).getYears();
        if (age < category.getMinAge() || age > category.getMaxAge()) {
            throw new AthleteOutsideCategoryException("Athlete age is outside the category age range");
        }
    }

    private TeamRegistration findTeamRegistration(Long id) {
        return teamRegistrationRepository.findById(id)
                .orElseThrow(() -> new TeamRegistrationNotFoundException("Team registration not found"));
    }

    private Athlete findAthlete(Long id) {
        return athleteRepository.findById(id)
                .orElseThrow(() -> new AthleteNotFoundException("Athlete not found"));
    }
}
