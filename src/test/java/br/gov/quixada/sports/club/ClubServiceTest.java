package br.gov.quixada.sports.club;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import br.gov.quixada.sports.club.exception.InactiveClubException;
import br.gov.quixada.sports.club.exception.ClubNotFoundException;
import br.gov.quixada.sports.team.TeamRepository;
import br.gov.quixada.sports.common.error.ActiveReferenceException;
import br.gov.quixada.sports.team.TeamStatus;
import br.gov.quixada.sports.shared.Address;
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
@DisplayName("ClubService")
class ClubServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private ClubRepository repository;

    @Mock
    private TeamRepository teamRepository;

    private ClubService service;

    @BeforeEach
    void setUp() {
        service = new ClubService(repository, teamRepository);
    }

    private Club aClub(ClubStatus status) {
        return Club.builder()
                .name("Club A")
                .manager("John")
                .phone("88999999999")
                .address(new Address("A Street", "10", "Downtown", "Quixadá", "63900-000"))
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should find all when name is null")
        void shouldFindAllWhenNameIsNull() {
            Page<Club> expected = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(expected);

            Page<Club> result = service.findAll(null, null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("should find all when name is blank")
        void shouldFindAllWhenNameIsBlank() {
            Page<Club> expected = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(expected);

            Page<Club> result = service.findAll("   ", null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("should filter by name when provided")
        void shouldFilterByNameWhenProvided() {
            Page<Club> expected = Page.empty();
            when(repository.findByNameContainingIgnoreCase("club", PAGEABLE)).thenReturn(expected);

            Page<Club> result = service.findAll("club", null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findByNameContainingIgnoreCase("club", PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("should return the club when it exists")
        void shouldReturnClubWhenItExists() {
            Club club = aClub(ClubStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(club));

            Club result = service.findByIdOrThrowNotFound(1L);

            assertThat(result).isSameAs(club);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("should throw ClubNotFoundException when it does not exist")
        void shouldThrowNotFoundWhenItDoesNotExist() {
            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(ClubNotFoundException.class)
                    .hasMessage("Club not found");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should force ACTIVE status and save")
        void shouldForceActiveStatusAndSave() {
            Club club = aClub(ClubStatus.INACTIVE);
            when(repository.save(any(Club.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Club saved = service.save(club);

            assertThat(saved).isSameAs(club);
            assertThat(saved.getStatus()).isEqualTo(ClubStatus.ACTIVE);
            verify(repository).save(club);
        }
    }

    @Nested
    @DisplayName("activate / deactivate")
    class ActivateDeactivate {

        @Test
        @DisplayName("should activate when INACTIVE")
        void shouldActivateWhenInactive() {
            Club club = aClub(ClubStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(club));

            Club result = service.activate(1L);

            assertThat(result.getStatus()).isEqualTo(ClubStatus.ACTIVE);
            verify(repository, never()).save(any(Club.class));
        }

        @Test
        @DisplayName("should keep ACTIVE when already ACTIVE (idempotent)")
        void shouldKeepActiveWhenAlreadyActive() {
            Club club = aClub(ClubStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(club));

            Club result = service.activate(1L);

            assertThat(result.getStatus()).isEqualTo(ClubStatus.ACTIVE);
            verify(repository, never()).save(any(Club.class));
        }

        @Test
        @DisplayName("should throw ClubNotFoundException when activating a nonexistent id")
        void shouldThrowNotFoundWhenActivatingNonexistentId() {
            assertThatThrownBy(() -> service.activate(1L))
                    .isInstanceOf(ClubNotFoundException.class)
                    .hasMessage("Club not found");
        }

        @Test
        @DisplayName("should deactivate when ACTIVE")
        void shouldDeactivateWhenActive() {
            Club club = aClub(ClubStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(club));

            Club result = service.deactivate(1L);

            assertThat(result.getStatus()).isEqualTo(ClubStatus.INACTIVE);
            verify(repository, never()).save(any(Club.class));
        }

        @Test
        @DisplayName("should keep INACTIVE when already INACTIVE (idempotent)")
        void shouldKeepInactiveWhenAlreadyInactive() {
            Club club = aClub(ClubStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(club));

            Club result = service.deactivate(1L);

            assertThat(result.getStatus()).isEqualTo(ClubStatus.INACTIVE);
            verify(repository, never()).save(any(Club.class));
        }

        @Test
        @DisplayName("should throw ClubNotFoundException when deactivating a nonexistent id")
        void shouldThrowNotFoundWhenDeactivatingNonexistentId() {
            assertThatThrownBy(() -> service.deactivate(1L))
                    .isInstanceOf(ClubNotFoundException.class)
                    .hasMessage("Club not found");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        private Club newData() {
            return Club.builder()
                    .name("Club B")
                    .manager("Jane")
                    .phone("88888888888")
                    .address(new Address("B Street", "20", "Neighborhood Novo", "Fortaleza", "60000-000"))
                    .build();
        }

        @Test
        @DisplayName("should copy the data and not call save")
        void shouldUpdateDetailsWhenActive() {
            Club existing = aClub(ClubStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            Club result = service.update(1L, newData());

            assertThat(result).isSameAs(existing);
            assertThat(result.getName()).isEqualTo("Club B");
            assertThat(result.getManager()).isEqualTo("Jane");
            assertThat(result.getPhone()).isEqualTo("88888888888");
            assertThat(result.getAddress().getCity()).isEqualTo("Fortaleza");
            verify(repository, never()).save(any(Club.class));
        }

        @Test
        @DisplayName("should throw InactiveClubException when the club is INACTIVE")
        void shouldThrowInactiveWhenUpdatingInactive() {
            Club existing = aClub(ClubStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.update(1L, newData()))
                    .isInstanceOf(InactiveClubException.class)
                    .hasMessage("Club is inactive; reactivate it before editing");
        }

        @Test
        @DisplayName("should throw ClubNotFoundException when the id does not exist")
        void shouldThrowNotFoundWhenUpdatingNonexistentId() {
            assertThatThrownBy(() -> service.update(1L, newData()))
                    .isInstanceOf(ClubNotFoundException.class)
                    .hasMessage("Club not found");
        }
    }

    @Test
    @DisplayName("should not deactivate a club with an active team")
    void shouldNotDeactivateWithActiveTeam() {
        Club club = Club.builder().id(1L).status(ClubStatus.ACTIVE).build();
        when(repository.findById(1L)).thenReturn(Optional.of(club));
        when(teamRepository.existsByClubIdAndStatus(1L, TeamStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.deactivate(1L))
                .isInstanceOf(ActiveReferenceException.class);
    }
}
