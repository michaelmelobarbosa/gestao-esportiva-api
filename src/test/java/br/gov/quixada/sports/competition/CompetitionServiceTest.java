package br.gov.quixada.sports.competition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import br.gov.quixada.sports.competition.exception.CompetitionNotEditableException;
import br.gov.quixada.sports.competition.exception.CompetitionNotFoundException;
import br.gov.quixada.sports.competition.exception.InvalidPeriodException;
import br.gov.quixada.sports.competition.exception.CompetitionStatusTransitionException;
import br.gov.quixada.sports.registration.TeamRegistrationService;
import br.gov.quixada.sports.modality.ModalityStatus;
import br.gov.quixada.sports.modality.exception.InactiveModalityException;
import br.gov.quixada.sports.modality.Modality;
import br.gov.quixada.sports.match.MatchService;
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
@DisplayName("CompetitionService")
class CompetitionServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private CompetitionRepository repository;

    @Mock
    private ModalityRepository modalityRepository;

    @Mock
    private TeamRegistrationService teamRegistrationService;

    @Mock
    private MatchService matchService;

    private CompetitionService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Fortaleza"));
        service = new CompetitionService(repository, modalityRepository, teamRegistrationService, matchService, clock);
    }

    private Competition aCompetition(CompetitionStatus status) {
        return Competition.builder()
                .name("Test Cup")
                .year(2026)
                .registrationStartDate(LocalDate.of(2025, 11, 1))
                .registrationEndDate(LocalDate.of(2025, 12, 1))
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 1, 31))
                .status(status)
                .build();
    }

    private Modality aModality() {
        return Modality.builder()
                .name("Soccer")
                .status(br.gov.quixada.sports.modality.ModalityStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should find all when name is null")
        void shouldFindAllWhenNameIsNull() {
            Page<Competition> expected = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(expected);

            Page<Competition> result = service.findAll(null, null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("should find all when name is blank")
        void shouldFindAllWhenNameIsBlank() {
            Page<Competition> expected = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(expected);

            Page<Competition> result = service.findAll("   ", null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("should filter by name when provided")
        void shouldFilterByNameWhenProvided() {
            Page<Competition> expected = Page.empty();
            when(repository.findByNameContainingIgnoreCase("copa", PAGEABLE)).thenReturn(expected);

            Page<Competition> result = service.findAll("copa", null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findByNameContainingIgnoreCase("copa", PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("should return the competition when it exists")
        void shouldReturnCompetitionWhenItExists() {
            Competition competition = aCompetition(CompetitionStatus.PLANNING);
            when(repository.findById(1L)).thenReturn(Optional.of(competition));

            Competition result = service.findByIdOrThrowNotFound(1L);

            assertThat(result).isSameAs(competition);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("should throw CompetitionNotFoundException when it does not exist")
        void shouldThrowNotFoundWhenItDoesNotExist() {
            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(CompetitionNotFoundException.class)
                    .hasMessage("Competition not found");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should validate the period, set the modality and save")
        void shouldSaveWithModality() {
            Competition competition = aCompetition(CompetitionStatus.PLANNING);
            Modality modality = aModality();
            when(modalityRepository.findById(1L)).thenReturn(Optional.of(modality));
            when(repository.save(any(Competition.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Competition saved = service.save(competition, 1L);

            assertThat(saved).isSameAs(competition);
            assertThat(saved.getModality()).isSameAs(modality);
            verify(repository).save(competition);
        }

        @Test
        @DisplayName("should throw InvalidPeriodException when endDate is not after startDate")
        void shouldThrowInvalidPeriodWhenEndDateNotAfterStartDate() {
            Competition competition = Competition.builder()
                    .name("Test Cup")
                    .year(2026)
                    .startDate(LocalDate.of(2026, 1, 31))
                    .endDate(LocalDate.of(2026, 1, 1))
                    .build();

            assertThatThrownBy(() -> service.save(competition, 1L))
                    .isInstanceOf(InvalidPeriodException.class)
                    .hasMessage("End date must be after the start date");
        }

        @Test
        @DisplayName("should throw ModalityNotFoundException when the modality does not exist")
        void shouldThrowNotFoundWhenModalityDoesNotExist() {
            Competition competition = aCompetition(CompetitionStatus.PLANNING);

            assertThatThrownBy(() -> service.save(competition, 99L))
                    .isInstanceOf(ModalityNotFoundException.class)
                    .hasMessage("Modality not found");
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        @DisplayName("should cancel and propagate the cancellation to the registrations")
        void shouldCancelCompetition() {
            Competition competition = aCompetition(CompetitionStatus.PLANNING);
            when(repository.findById(1L)).thenReturn(Optional.of(competition));

            Competition result = service.changeStatus(1L, CompetitionStatus.CANCELED);

            assertThat(result.getStatus()).isEqualTo(CompetitionStatus.CANCELED);
            verify(teamRegistrationService).cancelByCompetition(1L);
            verify(matchService).cancelByCompetition(1L);
        }

        @Test
        @DisplayName("should forbid a manual status change other than CANCELED")
        void shouldForbidManualStatusChange() {
            assertThatThrownBy(() -> service.changeStatus(1L, CompetitionStatus.REGISTRATION_OPEN))
                    .isInstanceOf(CompetitionStatusTransitionException.class);
        }

        @Test
        @DisplayName("should throw CompetitionNotFoundException when the id does not exist")
        void shouldThrowNotFoundWhenIdDoesNotExist() {
            assertThatThrownBy(() -> service.changeStatus(1L, CompetitionStatus.CANCELED))
                    .isInstanceOf(CompetitionNotFoundException.class)
                    .hasMessage("Competition not found");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        private Competition newData() {
            return Competition.builder()
                    .name("Copa Nova")
                    .year(2027)
                    .registrationStartDate(LocalDate.of(2026, 10, 1))
                    .registrationEndDate(LocalDate.of(2026, 11, 1))
                    .startDate(LocalDate.of(2027, 2, 1))
                    .endDate(LocalDate.of(2027, 2, 28))
                    .build();
        }

        @Test
        @DisplayName("should copy name, year and dates and not call save")
        void shouldUpdateDetailsWhenEditable() {
            Competition existing = aCompetition(CompetitionStatus.PLANNING);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            Competition result = service.update(1L, newData());

            assertThat(result).isSameAs(existing);
            assertThat(result.getName()).isEqualTo("Copa Nova");
            assertThat(result.getYear()).isEqualTo(2027);
            assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2027, 2, 1));
            assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2027, 2, 28));
            verify(repository, never()).save(any(Competition.class));
        }

        @Test
        @DisplayName("should throw CompetitionNotEditableException when COMPLETED")
        void shouldThrowNotEditableWhenCompleted() {
            Competition existing = aCompetition(CompetitionStatus.COMPLETED);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.update(1L, newData()))
                    .isInstanceOf(CompetitionNotEditableException.class)
                    .hasMessage("A completed or canceled competition cannot be edited");
        }

        @Test
        @DisplayName("should throw CompetitionNotEditableException when CANCELED")
        void shouldThrowNotEditableWhenCanceled() {
            Competition existing = aCompetition(CompetitionStatus.CANCELED);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.update(1L, newData()))
                    .isInstanceOf(CompetitionNotEditableException.class)
                    .hasMessage("A completed or canceled competition cannot be edited");
        }

        @Test
        @DisplayName("should throw InvalidPeriodException when the dates are invalid")
        void shouldThrowInvalidPeriodWhenDatesAreInvalid() {
            Competition existing = aCompetition(CompetitionStatus.PLANNING);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            Competition newData = Competition.builder()
                    .name("Copa Nova")
                    .year(2027)
                    .startDate(LocalDate.of(2027, 2, 28))
                    .endDate(LocalDate.of(2027, 2, 1))
                    .build();

            assertThatThrownBy(() -> service.update(1L, newData))
                    .isInstanceOf(InvalidPeriodException.class)
                    .hasMessage("End date must be after the start date");
        }

        @Test
        @DisplayName("should throw CompetitionNotFoundException when the id does not exist")
        void shouldThrowNotFoundWhenUpdatingNonexistentId() {
            assertThatThrownBy(() -> service.update(1L, newData()))
                    .isInstanceOf(CompetitionNotFoundException.class)
                    .hasMessage("Competition not found");
        }
    }

    @Nested
    @DisplayName("business rules")
    class BusinessRules {

        @Test
        @DisplayName("should block creation with an inactive modality")
        void shouldBlockCreationWithInactiveModality() {
            Modality modality = Modality.builder().id(1L).name("Soccer")
                    .status(ModalityStatus.INACTIVE).build();
            when(modalityRepository.findById(1L)).thenReturn(Optional.of(modality));

            assertThatThrownBy(() -> service.save(aCompetition(CompetitionStatus.PLANNING), 1L))
                    .isInstanceOf(InactiveModalityException.class);
        }

        @Test
        @DisplayName("should recalculate the status on create based on the dates")
        void shouldRecalculateStatusOnCreate() {
            Modality modality = aModality();
            when(modalityRepository.findById(1L)).thenReturn(Optional.of(modality));
            when(repository.save(any(Competition.class))).thenAnswer(invocation -> invocation.getArgument(0));
            Competition competition = aCompetition(CompetitionStatus.PLANNING);

            Competition saved = service.save(competition, 1L);

            assertThat(saved.getStatus()).isEqualTo(CompetitionStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("canceledAt")
    class CanceledAt {

        @Test
        @DisplayName("should record the date when canceling")
        void shouldRecordDateWhenCanceling() {
            Competition competition = aCompetition(CompetitionStatus.PLANNING);
            when(repository.findById(1L)).thenReturn(Optional.of(competition));

            Competition result = service.changeStatus(1L, CompetitionStatus.CANCELED);

            assertThat(result.getCanceledAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("updateNotes")
    class UpdateNotes {

        @Test
        @DisplayName("should save the free-form notes")
        void shouldSaveNotes() {
            Competition competition = aCompetition(CompetitionStatus.IN_PROGRESS);
            when(repository.findById(1L)).thenReturn(Optional.of(competition));

            Competition result = service.updateNotes(1L, "End extended due to rain");

            assertThat(result.getNotes()).isEqualTo("End extended due to rain");
        }

        @Test
        @DisplayName("should allow notes on a CANCELED competition")
        void shouldAllowNotesOnCanceledCompetition() {
            Competition competition = aCompetition(CompetitionStatus.CANCELED);
            when(repository.findById(1L)).thenReturn(Optional.of(competition));

            Competition result = service.updateNotes(1L, "Canceled due to lack of teams");

            assertThat(result.getNotes()).isEqualTo("Canceled due to lack of teams");
        }

        @Test
        @DisplayName("should clear the notes when null is passed")
        void shouldClearNotes() {
            Competition competition = aCompetition(CompetitionStatus.PLANNING);
            competition.updateNotes("old text");
            when(repository.findById(1L)).thenReturn(Optional.of(competition));

            Competition result = service.updateNotes(1L, null);

            assertThat(result.getNotes()).isNull();
        }

        @Test
        @DisplayName("should throw not found when the competition does not exist")
        void shouldThrowNotFound() {
            when(repository.findById(9L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateNotes(9L, "x"))
                    .isInstanceOf(CompetitionNotFoundException.class);
        }
    }
}
