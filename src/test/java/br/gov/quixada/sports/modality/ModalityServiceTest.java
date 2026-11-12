package br.gov.quixada.sports.modality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import br.gov.quixada.sports.competition.CompetitionRepository;
import br.gov.quixada.sports.team.TeamRepository;
import br.gov.quixada.sports.common.error.ActiveReferenceException;
import br.gov.quixada.sports.team.TeamStatus;
import br.gov.quixada.sports.modality.exception.InactiveModalityException;
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
@DisplayName("ModalityService")
class ModalityServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private ModalityRepository repository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private CompetitionRepository competitionRepository;

    private ModalityService service;

    @BeforeEach
    void setUp() {
        service = new ModalityService(repository, teamRepository, competitionRepository);
    }

    private Modality aModality(ModalityStatus status) {
        return Modality.builder()
                .name("Soccer")
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should find all when name is null")
        void shouldFindAllWhenNameIsNull() {
            Page<Modality> expected = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(expected);

            Page<Modality> result = service.findAll(null, null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("should find all when name is blank")
        void shouldFindAllWhenNameIsBlank() {
            Page<Modality> expected = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(expected);

            Page<Modality> result = service.findAll("   ", null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("should filter by name when provided")
        void shouldFilterByNameWhenProvided() {
            Page<Modality> expected = Page.empty();
            when(repository.findByNameContainingIgnoreCase("futebol", PAGEABLE)).thenReturn(expected);

            Page<Modality> result = service.findAll("futebol", null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findByNameContainingIgnoreCase("futebol", PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("should return the modality when it exists")
        void shouldReturnModalityWhenItExists() {
            Modality modality = aModality(ModalityStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(modality));

            Modality result = service.findByIdOrThrowNotFound(1L);

            assertThat(result).isSameAs(modality);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("should throw ModalityNotFoundException when it does not exist")
        void shouldThrowNotFoundWhenItDoesNotExist() {
            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(ModalityNotFoundException.class)
                    .hasMessage("Modality not found");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should force ACTIVE status and save")
        void shouldForceActiveStatusAndSave() {
            Modality modality = aModality(ModalityStatus.INACTIVE);
            when(repository.save(any(Modality.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Modality saved = service.save(modality);

            assertThat(saved).isSameAs(modality);
            assertThat(saved.getStatus()).isEqualTo(ModalityStatus.ACTIVE);
            verify(repository).save(modality);
        }
    }

    @Nested
    @DisplayName("activate / deactivate")
    class ActivateDeactivate {

        @Test
        @DisplayName("should activate when INACTIVE")
        void shouldActivateWhenInactive() {
            Modality modality = aModality(ModalityStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(modality));

            Modality result = service.activate(1L);

            assertThat(result.getStatus()).isEqualTo(ModalityStatus.ACTIVE);
            verify(repository, never()).save(any(Modality.class));
        }

        @Test
        @DisplayName("should keep ACTIVE when already ACTIVE (idempotent)")
        void shouldKeepActiveWhenAlreadyActive() {
            Modality modality = aModality(ModalityStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(modality));

            Modality result = service.activate(1L);

            assertThat(result.getStatus()).isEqualTo(ModalityStatus.ACTIVE);
            verify(repository, never()).save(any(Modality.class));
        }

        @Test
        @DisplayName("should throw ModalityNotFoundException when activating a nonexistent id")
        void shouldThrowNotFoundWhenActivatingNonexistentId() {
            assertThatThrownBy(() -> service.activate(1L))
                    .isInstanceOf(ModalityNotFoundException.class)
                    .hasMessage("Modality not found");
        }

        @Test
        @DisplayName("should deactivate when ACTIVE")
        void shouldDeactivateWhenActive() {
            Modality modality = aModality(ModalityStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(modality));

            Modality result = service.deactivate(1L);

            assertThat(result.getStatus()).isEqualTo(ModalityStatus.INACTIVE);
            verify(repository, never()).save(any(Modality.class));
        }

        @Test
        @DisplayName("should keep INACTIVE when already INACTIVE (idempotent)")
        void shouldKeepInactiveWhenAlreadyInactive() {
            Modality modality = aModality(ModalityStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(modality));

            Modality result = service.deactivate(1L);

            assertThat(result.getStatus()).isEqualTo(ModalityStatus.INACTIVE);
            verify(repository, never()).save(any(Modality.class));
        }

        @Test
        @DisplayName("should throw ModalityNotFoundException when deactivating a nonexistent id")
        void shouldThrowNotFoundWhenDeactivatingNonexistentId() {
            assertThatThrownBy(() -> service.deactivate(1L))
                    .isInstanceOf(ModalityNotFoundException.class)
                    .hasMessage("Modality not found");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should copy the name and not call save")
        void shouldUpdateNameWhenActive() {
            Modality existing = aModality(ModalityStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            Modality result = service.update(1L, Modality.builder().name("Basketball").build());

            assertThat(result).isSameAs(existing);
            assertThat(result.getName()).isEqualTo("Basketball");
            verify(repository, never()).save(any(Modality.class));
        }

        @Test
        @DisplayName("should throw InactiveModalityException when the modality is INACTIVE")
        void shouldThrowInactiveWhenUpdatingInactive() {
            Modality existing = aModality(ModalityStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.update(1L, Modality.builder().name("Basketball").build()))
                    .isInstanceOf(InactiveModalityException.class)
                    .hasMessage("Modality is inactive; reactivate it before editing");
        }

        @Test
        @DisplayName("should throw ModalityNotFoundException when the id does not exist")
        void shouldThrowNotFoundWhenUpdatingNonexistentId() {
            assertThatThrownBy(() -> service.update(1L, Modality.builder().name("Basketball").build()))
                    .isInstanceOf(ModalityNotFoundException.class)
                    .hasMessage("Modality not found");
        }
    }

    @Nested
    @DisplayName("business rules")
    class BusinessRules {

        @Test
        @DisplayName("should not deactivate a modality with an active team")
        void shouldNotDeactivateWithActiveTeam() {
            Modality modality = Modality.builder().id(1L).status(ModalityStatus.ACTIVE).build();
            when(repository.findById(1L)).thenReturn(Optional.of(modality));
            when(teamRepository.existsByModalityIdAndStatus(1L, TeamStatus.ACTIVE)).thenReturn(true);

            assertThatThrownBy(() -> service.deactivate(1L))
                    .isInstanceOf(ActiveReferenceException.class);
        }

        @Test
        @DisplayName("should not deactivate a modality with an active competition")
        void shouldNotDeactivateWithActiveCompetition() {
            Modality modality = Modality.builder().id(1L).status(ModalityStatus.ACTIVE).build();
            when(repository.findById(1L)).thenReturn(Optional.of(modality));
            when(teamRepository.existsByModalityIdAndStatus(1L, TeamStatus.ACTIVE)).thenReturn(false);
            when(competitionRepository.existsByModalityIdAndStatusNotIn(eq(1L), anyCollection())).thenReturn(true);

            assertThatThrownBy(() -> service.deactivate(1L))
                    .isInstanceOf(ActiveReferenceException.class);
        }
    }
}
