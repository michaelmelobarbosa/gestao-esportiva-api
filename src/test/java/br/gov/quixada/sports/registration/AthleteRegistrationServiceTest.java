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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import br.gov.quixada.sports.athlete.Athlete;
import br.gov.quixada.sports.athlete.AthleteRepository;
import br.gov.quixada.sports.athlete.exception.AthleteNotFoundException;
import br.gov.quixada.sports.category.Category;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionStatus;
import br.gov.quixada.sports.team.Team;
import br.gov.quixada.sports.registration.exception.AthleteOutsideCategoryException;
import br.gov.quixada.sports.registration.exception.AthleteAlreadyRegisteredException;
import br.gov.quixada.sports.registration.exception.AthleteRegistrationNotFoundException;
import br.gov.quixada.sports.registration.exception.LastActiveAthleteException;
import br.gov.quixada.sports.registration.exception.InvalidRegistrationException;
import br.gov.quixada.sports.registration.exception.TeamRegistrationNotFoundException;
import br.gov.quixada.sports.registration.exception.JerseyNumberAlreadyUsedException;
import br.gov.quixada.sports.registration.exception.InvalidStatusTransitionException;
import br.gov.quixada.sports.modality.Modality;
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
@DisplayName("AthleteRegistrationService")
class AthleteRegistrationServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);
    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");

    @Mock
    private AthleteRegistrationRepository repository;

    @Mock
    private TeamRegistrationRepository teamRegistrationRepository;

    @Mock
    private AthleteRepository athleteRepository;

    private Clock clock;

    private AthleteRegistrationService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZONE);
        service = new AthleteRegistrationService(repository, teamRegistrationRepository, athleteRepository, clock);
    }

    private Athlete anAthlete(LocalDate birthDate) {
        return Athlete.builder()
                .fullName("John de Tal")
                .birthDate(birthDate)
                .status(br.gov.quixada.sports.athlete.AthleteStatus.ACTIVE)
                .build();
    }

    private Modality aModality(Long id) {
        return Modality.builder().id(id).name("Soccer").build();
    }

    private Team aTeam(Long id, Long modalityId) {
        return Team.builder().id(id).name("Team A").modality(aModality(modalityId)).build();
    }

    private Category aCategory(Long id, Integer minAge, Integer maxAge) {
        Competition competition = Competition.builder().id(1L).name("Test Cup")
                .status(CompetitionStatus.REGISTRATION_OPEN).build();
        return Category.builder()
                .id(id)
                .name("Sub-20")
                .minAge(minAge)
                .maxAge(maxAge)
                .competition(competition)
                .status(br.gov.quixada.sports.category.CategoryStatus.ACTIVE)
                .build();
    }

    private TeamRegistration aTeamRegistration(Category category) {
        return TeamRegistration.builder()
                .id(1L)
                .competition(Competition.builder().id(1L).name("Test Cup")
                        .status(CompetitionStatus.REGISTRATION_OPEN).build())
                .status(RegistrationStatus.PENDING)
                .category(category)
                .team(aTeam(3L, 10L))
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should find all when the team registration id is null")
        void shouldFindAllWhenTeamRegistrationIdIsNull() {
            Page<AthleteRegistration> expected = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(expected);

            Page<AthleteRegistration> result = service.findAll(null, null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findAll(PAGEABLE);
        }

        @Test
        @DisplayName("should filter by team registration when informed")
        void shouldFilterByTeamRegistrationWhenInformed() {
            Page<AthleteRegistration> expected = Page.empty();
            when(repository.findByTeamRegistrationId(1L, PAGEABLE)).thenReturn(expected);

            Page<AthleteRegistration> result = service.findAll(1L, null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findByTeamRegistrationId(1L, PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("should return the registration when it exists")
        void shouldReturnRegistrationWhenExists() {
            AthleteRegistration registration = AthleteRegistration.builder().build();
            when(repository.findById(1L)).thenReturn(Optional.of(registration));

            AthleteRegistration result = service.findByIdOrThrowNotFound(1L);

            assertThat(result).isSameAs(registration);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("should throw AthleteRegistrationNotFoundException when it does not exist")
        void shouldThrowNotFoundWhenMissing() {
            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(AthleteRegistrationNotFoundException.class)
                    .hasMessage("Athlete registration not found");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should link team registration and athlete, set status PENDING and save")
        void shouldSaveWhenDataIsValid() {
            TeamRegistration teamRegistration = aTeamRegistration(aCategory(2L, 18, 40));
            Athlete athlete = anAthlete(LocalDate.of(2000, 6, 15));
            when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(teamRegistration));
            when(athleteRepository.findById(2L)).thenReturn(Optional.of(athlete));
            when(repository.save(any(AthleteRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

            AthleteRegistration registration = AthleteRegistration.builder().jerseyNumber("10").build();
            AthleteRegistration saved = service.save(registration, 1L, 2L);

            assertThat(saved).isSameAs(registration);
            assertThat(saved.getTeamRegistration()).isSameAs(teamRegistration);
            assertThat(saved.getAthlete()).isSameAs(athlete);
            assertThat(saved.getStatus()).isEqualTo(RegistrationStatus.PENDING);
            verify(repository).save(registration);
        }

        @Test
        @DisplayName("should throw TeamRegistrationNotFoundException when the team registration does not exist")
        void shouldThrowNotFoundWhenTeamRegistrationDoesNotExist() {
            AthleteRegistration registration = AthleteRegistration.builder().jerseyNumber("10").build();

            assertThatThrownBy(() -> service.save(registration, 99L, 2L))
                    .isInstanceOf(TeamRegistrationNotFoundException.class)
                    .hasMessage("Team registration not found");
        }

        @Test
        @DisplayName("should throw AthleteNotFoundException when the athlete does not exist")
        void shouldThrowNotFoundWhenAthleteDoesNotExist() {
            TeamRegistration teamRegistration = aTeamRegistration(aCategory(2L, 18, 40));
            when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(teamRegistration));
            AthleteRegistration registration = AthleteRegistration.builder().jerseyNumber("10").build();

            assertThatThrownBy(() -> service.save(registration, 1L, 99L))
                    .isInstanceOf(AthleteNotFoundException.class)
                    .hasMessage("Athlete not found");
        }

        @Test
        @DisplayName("should throw AthleteAlreadyRegisteredException when the athlete is already on this team")
        void shouldThrowAlreadyRegisteredWhenSameTeam() {
            TeamRegistration teamRegistration = aTeamRegistration(aCategory(2L, 18, 40));
            Athlete athlete = anAthlete(LocalDate.of(2000, 6, 15));
            when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(teamRegistration));
            when(athleteRepository.findById(2L)).thenReturn(Optional.of(athlete));
            when(repository.existsByTeamRegistrationIdAndAthleteIdAndStatusIn(1L, 2L, RegistrationStatus.OCCUPY_SLOT)).thenReturn(true);
            AthleteRegistration registration = AthleteRegistration.builder().jerseyNumber("10").build();

            assertThatThrownBy(() -> service.save(registration, 1L, 2L))
                    .isInstanceOf(AthleteAlreadyRegisteredException.class)
                    .hasMessage("Athlete is already registered on this team");
        }

        @Test
        @DisplayName("should throw AthleteAlreadyRegisteredException when the athlete is already on another team in the competition")
        void shouldThrowAlreadyRegisteredWhenOtherTeamInCompetition() {
            TeamRegistration teamRegistration = aTeamRegistration(aCategory(2L, 18, 40));
            Athlete athlete = anAthlete(LocalDate.of(2000, 6, 15));
            when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(teamRegistration));
            when(athleteRepository.findById(2L)).thenReturn(Optional.of(athlete));
            when(repository.existsByTeamRegistrationCompetitionIdAndAthleteIdAndStatusIn(1L, 2L, RegistrationStatus.OCCUPY_SLOT)).thenReturn(true);
            AthleteRegistration registration = AthleteRegistration.builder().jerseyNumber("10").build();

            assertThatThrownBy(() -> service.save(registration, 1L, 2L))
                    .isInstanceOf(AthleteAlreadyRegisteredException.class)
                    .hasMessage("Athlete is already registered on another team in this competition");
        }

        @Test
        @DisplayName("should throw JerseyNumberAlreadyUsedException when the jersey number is already in use")
        void shouldThrowJerseyNumberAlreadyUsed() {
            TeamRegistration teamRegistration = aTeamRegistration(aCategory(2L, 18, 40));
            Athlete athlete = anAthlete(LocalDate.of(2000, 6, 15));
            when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(teamRegistration));
            when(athleteRepository.findById(2L)).thenReturn(Optional.of(athlete));
            when(repository.existsByTeamRegistrationIdAndJerseyNumberAndStatusIn(1L, "10", RegistrationStatus.OCCUPY_SLOT)).thenReturn(true);
            AthleteRegistration registration = AthleteRegistration.builder().jerseyNumber("10").build();

            assertThatThrownBy(() -> service.save(registration, 1L, 2L))
                    .isInstanceOf(JerseyNumberAlreadyUsedException.class)
                    .hasMessage("Jersey number already used in this registration");
        }

        @Test
        @DisplayName("should throw AthleteOutsideCategoryException when the age is outside the range")
        void shouldThrowAthleteOutsideCategory() {
            TeamRegistration teamRegistration = aTeamRegistration(aCategory(2L, 30, 40));
            Athlete athlete = anAthlete(LocalDate.of(2000, 6, 15));
            when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(teamRegistration));
            when(athleteRepository.findById(2L)).thenReturn(Optional.of(athlete));
            AthleteRegistration registration = AthleteRegistration.builder().jerseyNumber("10").build();

            assertThatThrownBy(() -> service.save(registration, 1L, 2L))
                    .isInstanceOf(AthleteOutsideCategoryException.class)
                    .hasMessage("Athlete age is outside the category age range");
        }

        @Test
        @DisplayName("should skip the age validation when the athlete has no birth date")
        void shouldSaveWhenBirthDateIsNull() {
            TeamRegistration teamRegistration = aTeamRegistration(aCategory(2L, 30, 40));
            Athlete athlete = anAthlete(null);
            when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(teamRegistration));
            when(athleteRepository.findById(2L)).thenReturn(Optional.of(athlete));
            when(repository.save(any(AthleteRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

            AthleteRegistration registration = AthleteRegistration.builder().jerseyNumber("10").build();
            AthleteRegistration saved = service.save(registration, 1L, 2L);

            assertThat(saved).isSameAs(registration);
            assertThat(saved.getStatus()).isEqualTo(RegistrationStatus.PENDING);
            verify(repository).save(registration);
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        @DisplayName("should change the status and not call save")
        void shouldChangeStatus() {
            AthleteRegistration registration = AthleteRegistration.builder().status(RegistrationStatus.PENDING).build();
            when(repository.findById(1L)).thenReturn(Optional.of(registration));

            AthleteRegistration result = service.changeStatus(1L, RegistrationStatus.APPROVED);

            assertThat(result.getStatus()).isEqualTo(RegistrationStatus.APPROVED);
            verify(repository, never()).save(any(AthleteRegistration.class));
        }

        @Test
        @DisplayName("should keep the status when it is the same (idempotency)")
        void shouldKeepStatusWhenSame() {
            AthleteRegistration registration = AthleteRegistration.builder().status(RegistrationStatus.APPROVED).build();
            when(repository.findById(1L)).thenReturn(Optional.of(registration));

            AthleteRegistration result = service.changeStatus(1L, RegistrationStatus.APPROVED);

            assertThat(result.getStatus()).isEqualTo(RegistrationStatus.APPROVED);
            verify(repository, never()).save(any(AthleteRegistration.class));
        }

        @Test
        @DisplayName("should block transition from CANCELED")
        void shouldBlockTransitionFromCanceled() {
            AthleteRegistration registration = AthleteRegistration.builder().status(RegistrationStatus.CANCELED).build();
            when(repository.findById(1L)).thenReturn(Optional.of(registration));

            assertThatThrownBy(() -> service.changeStatus(1L, RegistrationStatus.APPROVED))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("should throw AthleteRegistrationNotFoundException when the id does not exist")
        void shouldThrowNotFoundWhenMissing() {
            assertThatThrownBy(() -> service.changeStatus(1L, RegistrationStatus.APPROVED))
                    .isInstanceOf(AthleteRegistrationNotFoundException.class)
                    .hasMessage("Athlete registration not found");
        }
    }

    @Nested
    @DisplayName("business rules")
    class BusinessRules {

        @Test
        @DisplayName("should block registering an inactive athlete")
        void shouldBlockInactiveAthlete() {
            TeamRegistration teamRegistration = aTeamRegistration(aCategory(2L, 18, 40));
            Athlete athlete = Athlete.builder()
                    .fullName("John")
                    .birthDate(LocalDate.of(2000, 6, 15))
                    .status(br.gov.quixada.sports.athlete.AthleteStatus.INACTIVE)
                    .build();
            when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(teamRegistration));
            when(athleteRepository.findById(2L)).thenReturn(Optional.of(athlete));

            assertThatThrownBy(() -> service.save(AthleteRegistration.builder().jerseyNumber("10").build(), 1L, 2L))
                    .isInstanceOf(InvalidRegistrationException.class)
                    .hasMessage("An inactive athlete cannot be registered");
        }

        @Test
        @DisplayName("should block registration in an inactive category")
        void shouldBlockInactiveCategory() {
            Competition competition = Competition.builder().id(1L).name("Copa")
                    .status(CompetitionStatus.REGISTRATION_OPEN).build();
            Category category = Category.builder()
                    .id(2L).name("Sub-20").minAge(18).maxAge(40)
                    .competition(competition)
                    .status(br.gov.quixada.sports.category.CategoryStatus.INACTIVE)
                    .build();
            TeamRegistration teamRegistration = TeamRegistration.builder()
                    .id(1L).competition(competition).category(category)
                    .team(aTeam(3L, 10L)).status(RegistrationStatus.PENDING)
                    .build();
            Athlete athlete = anAthlete(LocalDate.of(2000, 6, 15));
            when(teamRegistrationRepository.findById(1L)).thenReturn(Optional.of(teamRegistration));
            when(athleteRepository.findById(2L)).thenReturn(Optional.of(athlete));

            assertThatThrownBy(() -> service.save(AthleteRegistration.builder().jerseyNumber("10").build(), 1L, 2L))
                    .isInstanceOf(InvalidRegistrationException.class)
                    .hasMessage("An inactive category cannot receive registrations");
        }

        @Test
        @DisplayName("should not remove the last active athlete from an approved team")
        void shouldNotRemoveLastActiveAthlete() {
            TeamRegistration teamRegistration = aTeamRegistration(aCategory(2L, 18, 40));
            teamRegistration.changeStatus(RegistrationStatus.APPROVED, LocalDateTime.now());
            Athlete athlete = anAthlete(LocalDate.of(2000, 6, 15));
            AthleteRegistration registration = AthleteRegistration.builder()
                    .id(10L).status(RegistrationStatus.APPROVED)
                    .teamRegistration(teamRegistration).athlete(athlete).jerseyNumber("10")
                    .build();
            when(repository.findById(10L)).thenReturn(Optional.of(registration));
            when(repository.countByTeamRegistrationIdAndStatusIn(eq(1L), anyCollection())).thenReturn(1L);

            assertThatThrownBy(() -> service.changeStatus(10L, RegistrationStatus.CANCELED))
                    .isInstanceOf(LastActiveAthleteException.class);
        }

        @Test
        @DisplayName("should allow removing the athlete when another one is active on the team")
        void shouldAllowRemovalWhenAnotherAthleteIsActive() {
            TeamRegistration teamRegistration = aTeamRegistration(aCategory(2L, 18, 40));
            teamRegistration.changeStatus(RegistrationStatus.APPROVED, LocalDateTime.now());
            Athlete athlete = anAthlete(LocalDate.of(2000, 6, 15));
            AthleteRegistration registration = AthleteRegistration.builder()
                    .id(10L).status(RegistrationStatus.APPROVED)
                    .teamRegistration(teamRegistration).athlete(athlete).jerseyNumber("10")
                    .build();
            when(repository.findById(10L)).thenReturn(Optional.of(registration));
            when(repository.countByTeamRegistrationIdAndStatusIn(eq(1L), anyCollection())).thenReturn(2L);

            var result = service.changeStatus(10L, RegistrationStatus.CANCELED);

            assertThat(result.getStatus()).isEqualTo(RegistrationStatus.CANCELED);
        }
    }

    @Nested
    @DisplayName("updateNotes")
    class UpdateNotes {

        @Test
        @DisplayName("should store the free-form notes")
        void shouldStoreNotes() {
            AthleteRegistration registration = AthleteRegistration.builder()
                    .id(10L).status(RegistrationStatus.APPROVED).jerseyNumber("10")
                    .build();
            when(repository.findById(10L)).thenReturn(Optional.of(registration));

            AthleteRegistration result = service.updateNotes(10L, "Pending documentation");

            assertThat(result.getNotes()).isEqualTo("Pending documentation");
        }

        @Test
        @DisplayName("should clear the notes when null")
        void shouldClearNotes() {
            AthleteRegistration registration = AthleteRegistration.builder()
                    .id(10L).status(RegistrationStatus.CANCELED).jerseyNumber("10")
                    .notes("old text")
                    .build();
            when(repository.findById(10L)).thenReturn(Optional.of(registration));

            AthleteRegistration result = service.updateNotes(10L, null);

            assertThat(result.getNotes()).isNull();
        }

        @Test
        @DisplayName("should throw not found when the registration does not exist")
        void shouldThrowNotFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateNotes(99L, "x"))
                    .isInstanceOf(br.gov.quixada.sports.registration.exception.AthleteRegistrationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("canceledAt")
    class CanceledAt {

        @Test
        @DisplayName("should record the date when canceling")
        void shouldRecordDateWhenCanceling() {
            AthleteRegistration registration = AthleteRegistration.builder()
                    .id(10L).status(RegistrationStatus.APPROVED).jerseyNumber("10")
                    .build();
            when(repository.findById(10L)).thenReturn(Optional.of(registration));

            AthleteRegistration result = service.changeStatus(10L, RegistrationStatus.CANCELED);

            assertThat(result.getCanceledAt()).isNotNull();
        }
    }
}
