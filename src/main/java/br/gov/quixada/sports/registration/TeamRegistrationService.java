package br.gov.quixada.sports.registration;

import br.gov.quixada.sports.category.Category;
import br.gov.quixada.sports.category.CategoryRepository;
import br.gov.quixada.sports.category.CategoryStatus;
import br.gov.quixada.sports.category.exception.InactiveCategoryException;
import br.gov.quixada.sports.category.exception.CategoryNotFoundException;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionRepository;
import br.gov.quixada.sports.competition.CompetitionStatus;
import br.gov.quixada.sports.competition.exception.CompetitionNotFoundException;
import br.gov.quixada.sports.team.Team;
import br.gov.quixada.sports.team.TeamRepository;
import br.gov.quixada.sports.team.TeamStatus;
import br.gov.quixada.sports.team.exception.InactiveTeamException;
import br.gov.quixada.sports.team.exception.TeamNotFoundException;
import br.gov.quixada.sports.registration.exception.TeamAlreadyRegisteredException;
import br.gov.quixada.sports.registration.exception.TeamRegistrationNotFoundException;
import br.gov.quixada.sports.registration.exception.TeamRegistrationWithoutAthletesException;
import br.gov.quixada.sports.registration.exception.InvalidRegistrationException;
import br.gov.quixada.sports.registration.exception.RegistrationClosedException;
import br.gov.quixada.sports.registration.exception.InvalidStatusTransitionException;
import br.gov.quixada.sports.match.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamRegistrationService {

    private final TeamRegistrationRepository repository;
    private final AthleteRegistrationRepository athleteRegistrationRepository;
    private final CompetitionRepository competitionRepository;
    private final CategoryRepository categoryRepository;
    private final TeamRepository teamRepository;
    private final MatchService matchService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<TeamRegistration> findAll(Long competitionId, RegistrationStatus status, Pageable pageable) {
        if (competitionId == null) {
            return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        }
        return status == null
                ? repository.findByCompetitionId(competitionId, pageable)
                : repository.findByCompetitionIdAndStatus(competitionId, status, pageable);
    }

    @Transactional(readOnly = true)
    public TeamRegistration findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TeamRegistrationNotFoundException("Team registration not found"));
    }

    @Transactional
    public TeamRegistration save(TeamRegistration registration, Long competitionId, Long categoryId, Long teamId) {
        Competition competition = findCompetition(competitionId);
        Category category = findCategory(categoryId);
        Team team = findTeam(teamId);

        if (competition.calculateStatus(LocalDate.now(clock)) != CompetitionStatus.REGISTRATION_OPEN) {
            throw new RegistrationClosedException("Competition registration is not open");
        }
        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new InactiveTeamException("Team is inactive and cannot be registered");
        }
        if (category.getStatus() != CategoryStatus.ACTIVE) {
            throw new InactiveCategoryException("Category is inactive and cannot receive registrations");
        }
        if (!category.getCompetition().getId().equals(competition.getId())) {
            throw new InvalidRegistrationException("Category does not belong to the informed competition");
        }
        if (!team.getModality().getId().equals(competition.getModality().getId())) {
            throw new InvalidRegistrationException("Team modality differs from the competition modality");
        }
        if (repository.existsByCompetitionIdAndTeamIdAndStatusIn(
                competitionId, teamId, RegistrationStatus.OCCUPY_SLOT)) {
            throw new TeamAlreadyRegisteredException("Team already has an active registration in this competition");
        }

        registration.setCompetition(competition);
        registration.setCategory(category);
        registration.setTeam(team);
        registration.setStatus(RegistrationStatus.PENDING);
        return repository.save(registration);
    }

    @Transactional
    public TeamRegistration changeStatus(Long id, RegistrationStatus status) {
        TeamRegistration registration = findByIdOrThrowNotFound(id);

        if (registration.getStatus() == status) {
            return registration;
        }
        if (!registration.getStatus().canTransitionTo(status)) {
            throw new InvalidStatusTransitionException(
                    "Invalid status transition: " + registration.getStatus() + " -> " + status);
        }

        if (status == RegistrationStatus.PENDING) {
            revalidateReactivation(registration);
        }
        if (status == RegistrationStatus.APPROVED) {
            validateMinimumAthletes(registration);
        }

        registration.changeStatus(status, LocalDateTime.now(clock));

        if (status == RegistrationStatus.CANCELED || status == RegistrationStatus.INACTIVE) {
            cascadeAthleteStatuss(registration.getId(), status);
            matchService.cancelByRegistration(registration.getId());
        }

        return registration;
    }

    /**
     * Free-form communication field: can be set, changed, or cleared in any
     * status, without business checks.
     */
    @Transactional
    public TeamRegistration updateNotes(Long id, String notes) {
        TeamRegistration registration = findByIdOrThrowNotFound(id);
        registration.updateNotes(notes);
        return registration;
    }

    @Transactional
    public void cancelByCompetition(Long competitionId) {
        List<TeamRegistration> registrations = repository.findByCompetitionId(competitionId);
        for (TeamRegistration registration : registrations) {
            if (registration.getStatus() == RegistrationStatus.CANCELED) {
                continue;
            }
            registration.changeStatus(RegistrationStatus.CANCELED, LocalDateTime.now(clock));
            cascadeAthleteStatuss(registration.getId(), RegistrationStatus.CANCELED);
        }
    }

    private void revalidateReactivation(TeamRegistration registration) {
        Competition competition = registration.getCompetition();
        Category category = registration.getCategory();
        Team team = registration.getTeam();

        if (competition.calculateStatus(LocalDate.now(clock)) != CompetitionStatus.REGISTRATION_OPEN) {
            throw new RegistrationClosedException("Competition registration is not open");
        }
        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new InactiveTeamException("Team is inactive and cannot be reactivated");
        }
        if (category.getStatus() != CategoryStatus.ACTIVE) {
            throw new InactiveCategoryException("Category is inactive and cannot receive registrations");
        }
        if (!category.getCompetition().getId().equals(competition.getId())) {
            throw new InvalidRegistrationException("Category does not belong to the informed competition");
        }
        if (!team.getModality().getId().equals(competition.getModality().getId())) {
            throw new InvalidRegistrationException("Team modality differs from the competition modality");
        }
        if (repository.existsByCompetitionIdAndTeamIdAndStatusInAndIdNot(
                competition.getId(), team.getId(), RegistrationStatus.OCCUPY_SLOT, registration.getId())) {
            throw new TeamAlreadyRegisteredException("Team already has an active registration in this competition");
        }
    }

    private void validateMinimumAthletes(TeamRegistration registration) {
        long activeAthletes = athleteRegistrationRepository.countByTeamRegistrationIdAndStatusIn(
                registration.getId(), List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED));
        if (activeAthletes < 1) {
            throw new TeamRegistrationWithoutAthletesException(
                    "Team registration requires at least one athlete to be approved");
        }
    }

    private void cascadeAthleteStatuss(Long teamRegistrationId, RegistrationStatus status) {
        for (AthleteRegistration athleteRegistration : athleteRegistrationRepository.findByTeamRegistrationId(teamRegistrationId)) {
            RegistrationStatus atual = athleteRegistration.getStatus();

            if (atual == RegistrationStatus.CANCELED || atual == RegistrationStatus.REJECTED || atual == status) {
                continue;
            }
            if (status == RegistrationStatus.INACTIVE && atual != RegistrationStatus.PENDING
                    && atual != RegistrationStatus.APPROVED) {
                continue;
            }

            athleteRegistration.changeStatus(status, LocalDateTime.now(clock));
        }
    }

    private Competition findCompetition(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new CompetitionNotFoundException("Competition not found"));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
    }

    private Team findTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException("Team not found"));
    }
}
