package br.gov.quixada.sports.match;

import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionStatus;
import br.gov.quixada.sports.registration.TeamRegistration;
import br.gov.quixada.sports.registration.TeamRegistrationRepository;
import br.gov.quixada.sports.registration.RegistrationStatus;
import br.gov.quixada.sports.registration.exception.TeamRegistrationNotFoundException;
import br.gov.quixada.sports.match.dto.MatchCreateRequest;
import br.gov.quixada.sports.match.dto.MatchResultRequest;
import br.gov.quixada.sports.match.dto.MatchUpdateRequest;
import br.gov.quixada.sports.match.exception.InvalidMatchException;
import br.gov.quixada.sports.match.exception.MatchNotFoundException;
import br.gov.quixada.sports.match.exception.MatchStatusTransitionException;
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
public class MatchService {

    private static final List<MatchStatus> CANCELABLE_STATUSES =
            List.of(MatchStatus.SCHEDULED, MatchStatus.IN_PROGRESS);

    private final MatchRepository repository;
    private final TeamRegistrationRepository teamRegistrationRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<Match> findAll(Long competitionId, Long categoryId, MatchStatus status, Pageable pageable) {
        return repository.search(competitionId, categoryId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Match findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new MatchNotFoundException("Match not found"));
    }

    @Transactional
    public Match create(MatchCreateRequest request) {
        var home = findRegistration(request.homeRegistrationId());
        var away = findRegistration(request.awayRegistrationId());
        validateMatchData(request.round(), request.scheduledAt(), home, away, -1L);

        var initialStatus = home.getCompetition().calculateStatus(LocalDate.now(clock))
                == CompetitionStatus.CANCELED
                ? MatchStatus.CANCELED
                : MatchStatus.SCHEDULED;

        var match = Match.builder()
                .round(request.round())
                .scheduledAt(request.scheduledAt())
                .venue(request.venue())
                .notes(request.notes())
                .homeRegistration(home)
                .awayRegistration(away)
                .build();
        match.changeStatus(initialStatus, LocalDateTime.now(clock));
        return repository.save(match);
    }

    @Transactional
    public Match update(Long id, MatchUpdateRequest request) {
        var match = findByIdOrThrowNotFound(id);
        if (match.getStatus() != MatchStatus.SCHEDULED) {
            throw new InvalidMatchException("Only matches with SCHEDULED status can be edited");
        }

        var home = findRegistration(request.homeRegistrationId());
        var away = findRegistration(request.awayRegistrationId());
        validateMatchData(request.round(), request.scheduledAt(), home, away, id);

        match.updateDetails(request.round(), request.scheduledAt(), request.venue(), home, away);
        return match;
    }

    @Transactional
    public Match changeStatus(Long id, MatchStatus status) {
        var match = findByIdOrThrowNotFound(id);

        if (status == MatchStatus.COMPLETED) {
            throw new InvalidMatchException("Use the result endpoint to finish the match");
        }
        if (!match.getStatus().canTransitionTo(status)) {
            throw new MatchStatusTransitionException(
                    "Invalid status transition: " + match.getStatus() + " -> " + status);
        }

        match.changeStatus(status, LocalDateTime.now(clock));
        return match;
    }

    /**
     * Free-form communication field: can be set, changed, or cleared in any
     * status, without business checks.
     */
    @Transactional
    public Match updateNotes(Long id, String notes) {
        Match match = findByIdOrThrowNotFound(id);
        match.updateNotes(notes);
        return match;
    }

    @Transactional
    public Match recordResult(Long id, MatchResultRequest request) {
        var match = findByIdOrThrowNotFound(id);
        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new InvalidMatchException("Results can only be recorded for an IN_PROGRESS match");
        }
        if (request.homeGoals() == null || request.awayGoals() == null
                || request.homeGoals() < 0 || request.awayGoals() < 0) {
            throw new InvalidMatchException("Goals cannot be null or negative");
        }

        match.recordResult(request.homeGoals(), request.awayGoals());
        return match;
    }

    @Transactional
    public void cancelByCompetition(Long competitionId) {
        repository.cancelByCompetition(competitionId, CANCELABLE_STATUSES, MatchStatus.CANCELED,
                LocalDateTime.now(clock));
    }

    @Transactional
    public void cancelByRegistration(Long registrationId) {
        repository.cancelByRegistration(registrationId, CANCELABLE_STATUSES, MatchStatus.CANCELED,
                LocalDateTime.now(clock));
    }

    private void validateMatchData(Integer round, LocalDateTime scheduledAt,
                              TeamRegistration home, TeamRegistration away, Long ignoredId) {
        if (home.getId().equals(away.getId())) {
            throw new InvalidMatchException("Home and away teams must be different");
        }
        if (home.getStatus() != RegistrationStatus.APPROVED || away.getStatus() != RegistrationStatus.APPROVED) {
            throw new InvalidMatchException("Both teams must have an APPROVED registration");
        }
        if (!home.getCompetition().getId().equals(away.getCompetition().getId())) {
            throw new InvalidMatchException("The teams belong to different competitions");
        }
        if (!home.getCategory().getId().equals(away.getCategory().getId())) {
            throw new InvalidMatchException("The teams belong to different categories");
        }

        Competition competition = home.getCompetition();
        LocalDate matchDate = scheduledAt.toLocalDate();
        if (matchDate.isBefore(competition.getStartDate()) || matchDate.isAfter(competition.getEndDate())) {
            throw new InvalidMatchException("Match date must be within the competition period");
        }

        if (repository.existsConfrontoNaRound(home.getId(), away.getId(), round,
                ignoredId, MatchStatus.CANCELED)) {
            throw new InvalidMatchException("A match already exists between these teams in this round");
        }

        if (repository.existsTeamNaRound(List.of(home.getId(), away.getId()), round,
                ignoredId, MatchStatus.CANCELED)) {
            throw new InvalidMatchException("One of the teams already has a match in this round");
        }

        if (repository.existsConflitoScheduledAt(List.of(home.getId(), away.getId()), scheduledAt,
                ignoredId, MatchStatus.CANCELED)) {
            throw new InvalidMatchException("One of the teams already has a match at this time");
        }
    }

    private TeamRegistration findRegistration(Long id) {
        return teamRegistrationRepository.findById(id)
                .orElseThrow(() -> new TeamRegistrationNotFoundException("Team registration not found"));
    }
}
