package br.gov.quixada.sports.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import br.gov.quixada.sports.club.Club;
import br.gov.quixada.sports.club.ClubStatus;
import br.gov.quixada.sports.club.ClubRepository;
import br.gov.quixada.sports.club.exception.ClubNotFoundException;
import br.gov.quixada.sports.team.exception.InactiveTeamException;
import br.gov.quixada.sports.team.exception.TeamNotFoundException;
import br.gov.quixada.sports.registration.TeamRegistrationRepository;
import br.gov.quixada.sports.club.exception.InactiveClubException;
import br.gov.quixada.sports.common.error.ActiveReferenceException;
import br.gov.quixada.sports.registration.RegistrationStatus;
import br.gov.quixada.sports.modality.Modality;
import br.gov.quixada.sports.modality.ModalityStatus;
import br.gov.quixada.sports.modality.ModalityRepository;
import br.gov.quixada.sports.modality.exception.ModalityNotFoundException;
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
@DisplayName("TeamService")
class TeamServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private TeamRepository repository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ModalityRepository modalityRepository;

    @Mock
    private TeamRegistrationRepository teamRegistrationRepository;

    private TeamService service;

    @BeforeEach
    void setUp() {
        service = new TeamService(repository, clubRepository, modalityRepository, teamRegistrationRepository);
    }

    private Team aTeam(TeamStatus status) {
        return Team.builder()
                .name("Team A")
                .manager("John")
                .status(status)
                .build();
    }

    private Club aClub() {
        return Club.builder()
                .name("Club A")
                .status(ClubStatus.ACTIVE)
                .build();
    }

    private Modality aModality() {
        return Modality.builder()
                .name("Soccer")
                .status(ModalityStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should find all when name is null")
        void shouldFindAllWhenNameIsNull() {
            Page<Team> expected = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(expected);

            Page<Team> result = service.findAll(null, null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("should find all when name is blank")
        void shouldFindAllWhenNameIsBlank() {
            Page<Team> expected = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(expected);

            Page<Team> result = service.findAll("   ", null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("should filter by name when provided")
        void shouldFilterByNameWhenProvided() {
            Page<Team> expected = Page.empty();
            when(repository.findByNameContainingIgnoreCase("team", PAGEABLE)).thenReturn(expected);

            Page<Team> result = service.findAll("team", null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findByNameContainingIgnoreCase("team", PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("should return the team when it exists")
        void shouldReturnTeamWhenItExists() {
            Team team = aTeam(TeamStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(team));

            Team result = service.findByIdOrThrowNotFound(1L);

            assertThat(result).isSameAs(team);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("should throw TeamNotFoundException when it does not exist")
        void shouldThrowNotFoundWhenItDoesNotExist() {
            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(TeamNotFoundException.class)
                    .hasMessage("Team not found");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should activate, set the club and modality and save")
        void shouldSaveWithClubAndModality() {
            Team team = aTeam(TeamStatus.INACTIVE);
            Club club = aClub();
            Modality modality = aModality();
            when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
            when(modalityRepository.findById(2L)).thenReturn(Optional.of(modality));
            when(repository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Team saved = service.save(team, 1L, 2L);

            assertThat(saved).isSameAs(team);
            assertThat(saved.getStatus()).isEqualTo(TeamStatus.ACTIVE);
            assertThat(saved.getClub()).isSameAs(club);
            assertThat(saved.getModality()).isSameAs(modality);
            verify(repository).save(team);
        }

        @Test
        @DisplayName("should throw ClubNotFoundException when the club does not exist")
        void shouldThrowNotFoundWhenClubDoesNotExist() {
            Team team = aTeam(TeamStatus.ACTIVE);

            assertThatThrownBy(() -> service.save(team, 99L, 2L))
                    .isInstanceOf(ClubNotFoundException.class)
                    .hasMessage("Club not found");
        }

        @Test
        @DisplayName("should throw ModalityNotFoundException when the modality does not exist")
        void shouldThrowNotFoundWhenModalityDoesNotExist() {
            Team team = aTeam(TeamStatus.ACTIVE);
            when(clubRepository.findById(1L)).thenReturn(Optional.of(aClub()));

            assertThatThrownBy(() -> service.save(team, 1L, 99L))
                    .isInstanceOf(ModalityNotFoundException.class)
                    .hasMessage("Modality not found");
        }
    }

    @Nested
    @DisplayName("activate / deactivate")
    class ActivateDeactivate {

        @Test
        @DisplayName("should activate when INACTIVE")
        void shouldActivateWhenInactive() {
            Team team = aTeam(TeamStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(team));

            Team result = service.activate(1L);

            assertThat(result.getStatus()).isEqualTo(TeamStatus.ACTIVE);
            verify(repository, never()).save(any(Team.class));
        }

        @Test
        @DisplayName("should keep ACTIVE when already ACTIVE (idempotent)")
        void shouldKeepActiveWhenAlreadyActive() {
            Team team = aTeam(TeamStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(team));

            Team result = service.activate(1L);

            assertThat(result.getStatus()).isEqualTo(TeamStatus.ACTIVE);
            verify(repository, never()).save(any(Team.class));
        }

        @Test
        @DisplayName("should throw TeamNotFoundException when activating a nonexistent id")
        void shouldThrowNotFoundWhenActivatingNonexistentId() {
            assertThatThrownBy(() -> service.activate(1L))
                    .isInstanceOf(TeamNotFoundException.class)
                    .hasMessage("Team not found");
        }

        @Test
        @DisplayName("should deactivate when ACTIVE")
        void shouldDeactivateWhenActive() {
            Team team = aTeam(TeamStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(team));

            Team result = service.deactivate(1L);

            assertThat(result.getStatus()).isEqualTo(TeamStatus.INACTIVE);
            verify(repository, never()).save(any(Team.class));
        }

        @Test
        @DisplayName("should keep INACTIVE when already INACTIVE (idempotent)")
        void shouldKeepInactiveWhenAlreadyInactive() {
            Team team = aTeam(TeamStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(team));

            Team result = service.deactivate(1L);

            assertThat(result.getStatus()).isEqualTo(TeamStatus.INACTIVE);
            verify(repository, never()).save(any(Team.class));
        }

        @Test
        @DisplayName("should throw TeamNotFoundException when deactivating a nonexistent id")
        void shouldThrowNotFoundWhenDeactivatingNonexistentId() {
            assertThatThrownBy(() -> service.deactivate(1L))
                    .isInstanceOf(TeamNotFoundException.class)
                    .hasMessage("Team not found");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        private Team newData() {
            return Team.builder()
                    .name("Team B")
                    .manager("Jane")
                    .build();
        }

        @Test
        @DisplayName("should copy name and manager and not call save")
        void shouldUpdateDetailsWhenActive() {
            Team existing = aTeam(TeamStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            Team result = service.update(1L, newData());

            assertThat(result).isSameAs(existing);
            assertThat(result.getName()).isEqualTo("Team B");
            assertThat(result.getManager()).isEqualTo("Jane");
            verify(repository, never()).save(any(Team.class));
        }

        @Test
        @DisplayName("should throw InactiveTeamException when the team is INACTIVE")
        void shouldThrowInactiveWhenUpdatingInactive() {
            Team existing = aTeam(TeamStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.update(1L, newData()))
                    .isInstanceOf(InactiveTeamException.class)
                    .hasMessage("Team is inactive; reactivate it before editing");
        }

        @Test
        @DisplayName("should throw TeamNotFoundException when the id does not exist")
        void shouldThrowNotFoundWhenUpdatingNonexistentId() {
            assertThatThrownBy(() -> service.update(1L, newData()))
                    .isInstanceOf(TeamNotFoundException.class)
                    .hasMessage("Team not found");
        }
    }

    @Nested
    @DisplayName("business rules")
    class BusinessRules {

        @Test
        @DisplayName("should block a team with an inactive club")
        void shouldBlockTeamWithInactiveClub() {
            Club club = Club.builder().id(1L).name("Club A").status(ClubStatus.INACTIVE).build();
            when(clubRepository.findById(1L)).thenReturn(Optional.of(club));

            assertThatThrownBy(() -> service.save(aTeam(TeamStatus.ACTIVE), 1L, 2L))
                    .isInstanceOf(InactiveClubException.class);
        }

        @Test
        @DisplayName("should block a team with an inactive modality")
        void shouldBlockTeamWithInactiveModality() {
            Club club = Club.builder().id(1L).name("Club A").status(ClubStatus.ACTIVE).build();
            Modality modality = Modality.builder().id(2L).name("Soccer").status(ModalityStatus.INACTIVE).build();
            when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
            when(modalityRepository.findById(2L)).thenReturn(Optional.of(modality));

            assertThatThrownBy(() -> service.save(aTeam(TeamStatus.ACTIVE), 1L, 2L))
                    .isInstanceOf(br.gov.quixada.sports.modality.exception.InactiveModalityException.class);
        }

        @Test
        @DisplayName("should not deactivate a team with an active registration")
        void shouldNotDeactivateWithActiveRegistration() {
            Team team = Team.builder().id(1L).status(TeamStatus.ACTIVE).build();
            when(repository.findById(1L)).thenReturn(Optional.of(team));
            when(teamRegistrationRepository.existsByTeamIdAndStatusIn(1L, RegistrationStatus.OCCUPY_SLOT))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.deactivate(1L))
                    .isInstanceOf(ActiveReferenceException.class);
        }
    }
}
