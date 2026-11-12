package br.gov.quixada.sports.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import br.gov.quixada.sports.category.Category;
import br.gov.quixada.sports.category.CategoryStatus;
import br.gov.quixada.sports.category.CategoryRepository;
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
import br.gov.quixada.sports.modality.Modality;
import br.gov.quixada.sports.match.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamRegistrationService")
class TeamRegistrationServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private TeamRegistrationRepository repository;

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private AthleteRegistrationRepository athleteRegistrationRepository;

    @Mock
    private MatchService matchService;

    private TeamRegistrationService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Fortaleza"));
        service = new TeamRegistrationService(repository, athleteRegistrationRepository, competitionRepository,
                categoryRepository, teamRepository, matchService, clock);
    }

    private Modality aModality(Long id) {
        return Modality.builder().id(id).name("Soccer").build();
    }

    private Competition aCompetition(Long id, Long modalityId, CompetitionStatus status) {
        return Competition.builder()
                .id(id)
                .name("Test Cup")
                .year(2026)
                .modality(aModality(modalityId))
                .status(status)
                .build();
    }

    private Category aCategory(Long id, Competition competition) {
        return Category.builder()
                .id(id)
                .name("Sub-15")
                .minAge(13)
                .maxAge(15)
                .competition(competition)
                .status(br.gov.quixada.sports.category.CategoryStatus.ACTIVE)
                .build();
    }

    private Team aTeam(Long id, Long modalityId, TeamStatus status) {
        return Team.builder()
                .id(id)
                .name("Team A")
                .modality(aModality(modalityId))
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should find all when the competition id is null")
        void shouldFindAllWhenCompetitionIdIsNull() {
            Page<TeamRegistration> expected = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(expected);

            Page<TeamRegistration> result = service.findAll(null, null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findAll(PAGEABLE);
        }

        @Test
        @DisplayName("should filter by competition when informed")
        void shouldFilterByCompetitionWhenInformed() {
            Page<TeamRegistration> expected = Page.empty();
            when(repository.findByCompetitionId(1L, PAGEABLE)).thenReturn(expected);

            Page<TeamRegistration> result = service.findAll(1L, null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findByCompetitionId(1L, PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("should return the registration when it exists")
        void shouldReturnRegistrationWhenExists() {
            TeamRegistration registration = TeamRegistration.builder().build();
            when(repository.findById(1L)).thenReturn(Optional.of(registration));

            TeamRegistration result = service.findByIdOrThrowNotFound(1L);

            assertThat(result).isSameAs(registration);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("should throw TeamRegistrationNotFoundException when it does not exist")
        void shouldThrowNotFoundWhenMissing() {
            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(TeamRegistrationNotFoundException.class)
                    .hasMessage("Team registration not found");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should set competition, category, team and status PENDING and save")
        void shouldSaveWhenDataIsValid() {
            Competition competition = aCompetition(1L, 10L, CompetitionStatus.REGISTRATION_OPEN);
            Category category = aCategory(2L, competition);
            Team team = aTeam(3L, 10L, TeamStatus.ACTIVE);
            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
            when(teamRepository.findById(3L)).thenReturn(Optional.of(team));
            when(repository.save(any(TeamRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TeamRegistration registration = TeamRegistration.builder().build();
            TeamRegistration saved = service.save(registration, 1L, 2L, 3L);

            assertThat(saved).isSameAs(registration);
            assertThat(saved.getCompetition()).isSameAs(competition);
            assertThat(saved.getCategory()).isSameAs(category);
            assertThat(saved.getTeam()).isSameAs(team);
            assertThat(saved.getStatus()).isEqualTo(RegistrationStatus.PENDING);
            verify(repository).save(registration);
        }

        @Test
        @DisplayName("should throw CompetitionNotFoundException when the competition does not exist")
        void shouldThrowNotFoundWhenCompetitionDoesNotExist() {
            TeamRegistration registration = TeamRegistration.builder().build();

            assertThatThrownBy(() -> service.save(registration, 99L, 2L, 3L))
                    .isInstanceOf(CompetitionNotFoundException.class)
                    .hasMessage("Competition not found");
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when the category does not exist")
        void shouldThrowNotFoundWhenCategoryDoesNotExist() {
            Competition competition = aCompetition(1L, 10L, CompetitionStatus.REGISTRATION_OPEN);
            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            TeamRegistration registration = TeamRegistration.builder().build();

            assertThatThrownBy(() -> service.save(registration, 1L, 99L, 3L))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessage("Category not found");
        }

        @Test
        @DisplayName("should throw TeamNotFoundException when the team does not exist")
        void shouldThrowNotFoundWhenTeamDoesNotExist() {
            Competition competition = aCompetition(1L, 10L, CompetitionStatus.REGISTRATION_OPEN);
            Category category = aCategory(2L, competition);
            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
            TeamRegistration registration = TeamRegistration.builder().build();

            assertThatThrownBy(() -> service.save(registration, 1L, 2L, 99L))
                    .isInstanceOf(TeamNotFoundException.class)
                    .hasMessage("Team not found");
        }

        @Test
        @DisplayName("should throw RegistrationClosedException when competition registration is not open")
        void shouldThrowRegistrationClosed() {
            Competition competition = aCompetition(1L, 10L, CompetitionStatus.PLANNING);
            Category category = aCategory(2L, competition);
            Team team = aTeam(3L, 10L, TeamStatus.ACTIVE);
            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
            when(teamRepository.findById(3L)).thenReturn(Optional.of(team));
            TeamRegistration registration = TeamRegistration.builder().build();

            assertThatThrownBy(() -> service.save(registration, 1L, 2L, 3L))
                    .isInstanceOf(RegistrationClosedException.class)
                    .hasMessage("Competition registration is not open");
        }

        @Test
        @DisplayName("should throw InactiveTeamException when the team is INACTIVE")
        void shouldThrowTeamInactive() {
            Competition competition = aCompetition(1L, 10L, CompetitionStatus.REGISTRATION_OPEN);
            Category category = aCategory(2L, competition);
            Team team = aTeam(3L, 10L, TeamStatus.INACTIVE);
            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
            when(teamRepository.findById(3L)).thenReturn(Optional.of(team));
            TeamRegistration registration = TeamRegistration.builder().build();

            assertThatThrownBy(() -> service.save(registration, 1L, 2L, 3L))
                    .isInstanceOf(InactiveTeamException.class)
                    .hasMessage("Team is inactive and cannot be registered");
        }

        @Test
        @DisplayName("should throw InvalidRegistrationException when the category belongs to another competition")
        void shouldThrowInvalidWhenCategoryFromOtherCompetition() {
            Competition competition = aCompetition(1L, 10L, CompetitionStatus.REGISTRATION_OPEN);
            Category category = aCategory(2L, aCompetition(99L, 10L, CompetitionStatus.REGISTRATION_OPEN));
            Team team = aTeam(3L, 10L, TeamStatus.ACTIVE);
            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
            when(teamRepository.findById(3L)).thenReturn(Optional.of(team));
            TeamRegistration registration = TeamRegistration.builder().build();

            assertThatThrownBy(() -> service.save(registration, 1L, 2L, 3L))
                    .isInstanceOf(InvalidRegistrationException.class)
                    .hasMessage("Category does not belong to the informed competition");
        }

        @Test
        @DisplayName("should throw InvalidRegistrationException when the team modality differs")
        void shouldThrowInvalidWhenModalityDiffers() {
            Competition competition = aCompetition(1L, 10L, CompetitionStatus.REGISTRATION_OPEN);
            Category category = aCategory(2L, competition);
            Team team = aTeam(3L, 11L, TeamStatus.ACTIVE);
            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
            when(teamRepository.findById(3L)).thenReturn(Optional.of(team));
            TeamRegistration registration = TeamRegistration.builder().build();

            assertThatThrownBy(() -> service.save(registration, 1L, 2L, 3L))
                    .isInstanceOf(InvalidRegistrationException.class)
                    .hasMessage("Team modality differs from the competition modality");
        }

        @Test
        @DisplayName("should block duplicate registration of the same team in the competition")
        void shouldBlockDuplicateTeam() {
            Competition competition = aCompetition(1L, 10L, CompetitionStatus.REGISTRATION_OPEN);
            Category category = aCategory(2L, competition);
            Team team = aTeam(3L, 10L, TeamStatus.ACTIVE);
            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
            when(teamRepository.findById(3L)).thenReturn(Optional.of(team));
            when(repository.existsByCompetitionIdAndTeamIdAndStatusIn(1L, 3L, RegistrationStatus.OCCUPY_SLOT))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.save(TeamRegistration.builder().build(), 1L, 2L, 3L))
                    .isInstanceOf(TeamAlreadyRegisteredException.class)
                    .hasMessage("Team already has an active registration in this competition");
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        @DisplayName("should change the status and not call save")
        void shouldChangeStatus() {
            TeamRegistration registration = TeamRegistration.builder().id(1L).status(RegistrationStatus.PENDING).build();
            when(repository.findById(1L)).thenReturn(Optional.of(registration));
            when(athleteRegistrationRepository.countByTeamRegistrationIdAndStatusIn(
                    eq(1L), anyCollection())).thenReturn(1L);

            TeamRegistration result = service.changeStatus(1L, RegistrationStatus.APPROVED);

            assertThat(result.getStatus()).isEqualTo(RegistrationStatus.APPROVED);
            verify(repository, never()).save(any(TeamRegistration.class));
        }

        @Test
        @DisplayName("should keep the status when it is the same (idempotency)")
        void shouldKeepStatusWhenSame() {
            TeamRegistration registration = TeamRegistration.builder().status(RegistrationStatus.APPROVED).build();
            when(repository.findById(1L)).thenReturn(Optional.of(registration));

            TeamRegistration result = service.changeStatus(1L, RegistrationStatus.APPROVED);

            assertThat(result.getStatus()).isEqualTo(RegistrationStatus.APPROVED);
            verify(repository, never()).save(any(TeamRegistration.class));
        }

        @Test
        @DisplayName("should cascade cancel athletes and matches")
        void shouldCascadeCancel() {
            TeamRegistration registration = TeamRegistration.builder().id(1L).status(RegistrationStatus.PENDING).build();
            AthleteRegistration athlete = AthleteRegistration.builder().id(10L).status(RegistrationStatus.PENDING).build();
            when(repository.findById(1L)).thenReturn(Optional.of(registration));
            when(athleteRegistrationRepository.findByTeamRegistrationId(1L)).thenReturn(java.util.List.of(athlete));

            service.changeStatus(1L, RegistrationStatus.CANCELED);

            assertThat(athlete.getStatus()).isEqualTo(RegistrationStatus.CANCELED);
            verify(matchService).cancelByRegistration(1L);
        }

        @Test
        @DisplayName("should cascade deactivate athletes")
        void shouldCascadeDeactivate() {
            TeamRegistration registration = TeamRegistration.builder().id(1L).status(RegistrationStatus.APPROVED).build();
            AthleteRegistration athlete = AthleteRegistration.builder().id(10L).status(RegistrationStatus.PENDING).build();
            when(repository.findById(1L)).thenReturn(Optional.of(registration));
            when(athleteRegistrationRepository.findByTeamRegistrationId(1L)).thenReturn(java.util.List.of(athlete));

            service.changeStatus(1L, RegistrationStatus.INACTIVE);

            assertThat(athlete.getStatus()).isEqualTo(RegistrationStatus.INACTIVE);
            verify(matchService).cancelByRegistration(1L);
        }

        @Test
        @DisplayName("should block transition from CANCELED")
        void shouldBlockTransitionFromCanceled() {
            TeamRegistration registration = TeamRegistration.builder().id(1L).status(RegistrationStatus.CANCELED).build();
            when(repository.findById(1L)).thenReturn(Optional.of(registration));

            assertThatThrownBy(() -> service.changeStatus(1L, RegistrationStatus.PENDING))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("should throw TeamRegistrationNotFoundException when the id does not exist")
        void shouldThrowNotFoundWhenMissing() {
            assertThatThrownBy(() -> service.changeStatus(1L, RegistrationStatus.APPROVED))
                    .isInstanceOf(TeamRegistrationNotFoundException.class)
                    .hasMessage("Team registration not found");
        }
    }

    @Nested
    @DisplayName("business rules")
    class BusinessRules {

        private TeamRegistration fullRegistration(RegistrationStatus status, CompetitionStatus competitionStatus,
                                                  CategoryStatus categoryStatus, TeamStatus teamStatus) {
            Competition competition = aCompetition(1L, 10L, competitionStatus);
            Category category = Category.builder()
                    .id(2L)
                    .name("Sub-15")
                    .minAge(13)
                    .maxAge(15)
                    .competition(competition)
                    .status(categoryStatus)
                    .build();
            Team team = aTeam(3L, 10L, teamStatus);
            return TeamRegistration.builder()
                    .id(1L)
                    .status(status)
                    .competition(competition)
                    .category(category)
                    .team(team)
                    .build();
        }

        @Test
        @DisplayName("should reject approving a team without athletes")
        void shouldRejectApprovalWithoutAthletes() {
            TeamRegistration registration = fullRegistration(RegistrationStatus.PENDING,
                    CompetitionStatus.REGISTRATION_OPEN, CategoryStatus.ACTIVE, TeamStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(registration));
            when(athleteRegistrationRepository.countByTeamRegistrationIdAndStatusIn(eq(1L), anyCollection()))
                    .thenReturn(0L);

            assertThatThrownBy(() -> service.changeStatus(1L, RegistrationStatus.APPROVED))
                    .isInstanceOf(TeamRegistrationWithoutAthletesException.class)
                    .hasMessage("Team registration requires at least one athlete to be approved");
        }

        @Test
        @DisplayName("should reject reactivation with a closed competition")
        void shouldRejectReactivationWithClosedCompetition() {
            TeamRegistration registration = fullRegistration(RegistrationStatus.INACTIVE,
                    CompetitionStatus.PLANNING, CategoryStatus.ACTIVE, TeamStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(registration));

            assertThatThrownBy(() -> service.changeStatus(1L, RegistrationStatus.PENDING))
                    .isInstanceOf(RegistrationClosedException.class);
        }

        @Test
        @DisplayName("should reject reactivation with an inactive team")
        void shouldRejectReactivationWithInactiveTeam() {
            TeamRegistration registration = fullRegistration(RegistrationStatus.INACTIVE,
                    CompetitionStatus.REGISTRATION_OPEN, CategoryStatus.ACTIVE, TeamStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(registration));

            assertThatThrownBy(() -> service.changeStatus(1L, RegistrationStatus.PENDING))
                    .isInstanceOf(InactiveTeamException.class);
        }

        @Test
        @DisplayName("should reject reactivation with an inactive category")
        void shouldRejectReactivationWithInactiveCategory() {
            TeamRegistration registration = fullRegistration(RegistrationStatus.INACTIVE,
                    CompetitionStatus.REGISTRATION_OPEN, CategoryStatus.INACTIVE, TeamStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(registration));

            assertThatThrownBy(() -> service.changeStatus(1L, RegistrationStatus.PENDING))
                    .isInstanceOf(br.gov.quixada.sports.category.exception.InactiveCategoryException.class);
        }

        @Test
        @DisplayName("should reject reactivation when the team already has another active registration")
        void shouldRejectReactivationWithDuplicate() {
            TeamRegistration registration = fullRegistration(RegistrationStatus.INACTIVE,
                    CompetitionStatus.REGISTRATION_OPEN, CategoryStatus.ACTIVE, TeamStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(registration));
            when(repository.existsByCompetitionIdAndTeamIdAndStatusInAndIdNot(
                    1L, 3L, RegistrationStatus.OCCUPY_SLOT, 1L)).thenReturn(true);

            assertThatThrownBy(() -> service.changeStatus(1L, RegistrationStatus.PENDING))
                    .isInstanceOf(TeamAlreadyRegisteredException.class);
        }

        @Test
        @DisplayName("should not change REJECTED athletes in the INACTIVE cascade")
        void shouldNotChangeRejectedInInactiveCascade() {
            TeamRegistration registration = fullRegistration(RegistrationStatus.APPROVED,
                    CompetitionStatus.REGISTRATION_OPEN, CategoryStatus.ACTIVE, TeamStatus.ACTIVE);
            AthleteRegistration athlete = AthleteRegistration.builder()
                    .id(10L).status(RegistrationStatus.REJECTED).build();
            when(repository.findById(1L)).thenReturn(Optional.of(registration));
            when(athleteRegistrationRepository.findByTeamRegistrationId(1L)).thenReturn(List.of(athlete));

            service.changeStatus(1L, RegistrationStatus.INACTIVE);

            assertThat(athlete.getStatus()).isEqualTo(RegistrationStatus.REJECTED);
        }

        @Test
        @DisplayName("should block registration in an inactive category")
        void shouldBlockRegistrationInInactiveCategory() {
            Competition competition = aCompetition(1L, 10L, CompetitionStatus.REGISTRATION_OPEN);
            Category category = Category.builder().id(2L).name("Sub-15").minAge(13).maxAge(15)
                    .competition(competition).status(CategoryStatus.INACTIVE).build();
            Team team = aTeam(3L, 10L, TeamStatus.ACTIVE);
            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
            when(teamRepository.findById(3L)).thenReturn(Optional.of(team));

            assertThatThrownBy(() -> service.save(TeamRegistration.builder().build(), 1L, 2L, 3L))
                    .isInstanceOf(br.gov.quixada.sports.category.exception.InactiveCategoryException.class);
        }
    }

    @Nested
    @DisplayName("updateNotes")
    class UpdateNotes {

        @Test
        @DisplayName("should store the free-form notes on a CANCELED registration")
        void shouldStoreNotes() {
            TeamRegistration registration = TeamRegistration.builder().id(1L).status(RegistrationStatus.CANCELED).build();
            when(repository.findById(1L)).thenReturn(Optional.of(registration));

            TeamRegistration result = service.updateNotes(1L, "Team withdrew");

            assertThat(result.getNotes()).isEqualTo("Team withdrew");
            verify(repository, never()).save(any(TeamRegistration.class));
        }

        @Test
        @DisplayName("should clear the notes when blank")
        void shouldClearBlankNotes() {
            TeamRegistration registration = TeamRegistration.builder().id(1L).status(RegistrationStatus.PENDING)
                    .notes("old text").build();
            when(repository.findById(1L)).thenReturn(Optional.of(registration));

            TeamRegistration result = service.updateNotes(1L, "   ");

            assertThat(result.getNotes()).isNull();
        }

        @Test
        @DisplayName("should throw not found when the registration does not exist")
        void shouldThrowNotFound() {
            when(repository.findById(9L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateNotes(9L, "x"))
                    .isInstanceOf(TeamRegistrationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("canceledAt")
    class CanceledAt {

        @Test
        @DisplayName("should record the date when canceling")
        void shouldRecordDateWhenCanceling() {
            TeamRegistration registration = TeamRegistration.builder().id(1L).status(RegistrationStatus.APPROVED).build();
            when(repository.findById(1L)).thenReturn(Optional.of(registration));
            when(athleteRegistrationRepository.findByTeamRegistrationId(1L)).thenReturn(List.of());

            TeamRegistration result = service.changeStatus(1L, RegistrationStatus.CANCELED);

            assertThat(result.getCanceledAt()).isNotNull();
        }

        @Test
        @DisplayName("should not record the date on a transition other than CANCELED")
        void shouldNotRecordDateOnOtherTransition() {
            TeamRegistration registration = TeamRegistration.builder().id(1L).status(RegistrationStatus.PENDING).build();
            when(repository.findById(1L)).thenReturn(Optional.of(registration));
            when(athleteRegistrationRepository.countByTeamRegistrationIdAndStatusIn(
                    eq(1L), anyCollection())).thenReturn(1L);

            TeamRegistration result = service.changeStatus(1L, RegistrationStatus.APPROVED);

            assertThat(result.getCanceledAt()).isNull();
        }
    }
}
