package br.gov.quixada.sports.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import br.gov.quixada.sports.category.exception.InactiveCategoryException;
import br.gov.quixada.sports.category.exception.CategoryNotFoundException;
import br.gov.quixada.sports.category.exception.InvalidAgeException;
import br.gov.quixada.sports.category.exception.DuplicateCategoryException;
import br.gov.quixada.sports.common.error.ActiveReferenceException;
import br.gov.quixada.sports.registration.RegistrationStatus;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionStatus;
import br.gov.quixada.sports.competition.CompetitionRepository;
import br.gov.quixada.sports.competition.exception.CompetitionNotEditableException;
import br.gov.quixada.sports.competition.exception.CompetitionNotFoundException;
import br.gov.quixada.sports.registration.TeamRegistrationRepository;
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
@DisplayName("CategoryService")
class CategoryServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private CategoryRepository repository;

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private TeamRegistrationRepository teamRegistrationRepository;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Fortaleza"));
        service = new CategoryService(repository, competitionRepository, teamRegistrationRepository, clock);
    }

    private Category aCategory(CategoryStatus status) {
        return Category.builder()
                .name("Sub-15")
                .minAge(13)
                .maxAge(15)
                .status(status)
                .build();
    }

    private Competition aCompetition() {
        return Competition.builder()
                .name("Test Cup")
                .year(2026)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should find all when name is null")
        void shouldFindAllWhenNameIsNull() {
            Page<Category> expected = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(expected);

            Page<Category> result = service.findAll(null, null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("should find all when name is blank")
        void shouldFindAllWhenNameIsBlank() {
            Page<Category> expected = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(expected);

            Page<Category> result = service.findAll("   ", null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("should filter by name when provided")
        void shouldFilterByNameWhenProvided() {
            Page<Category> expected = Page.empty();
            when(repository.findByNameContainingIgnoreCase("sub", PAGEABLE)).thenReturn(expected);

            Page<Category> result = service.findAll("sub", null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findByNameContainingIgnoreCase("sub", PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("should return the category when it exists")
        void shouldReturnCategoryWhenItExists() {
            Category category = aCategory(CategoryStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(category));

            Category result = service.findByIdOrThrowNotFound(1L);

            assertThat(result).isSameAs(category);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when it does not exist")
        void shouldThrowNotFoundWhenItDoesNotExist() {
            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessage("Category not found");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should validate ages, force ACTIVE, set the competition and save")
        void shouldSaveWithCompetitionAndActiveStatus() {
            Category category = aCategory(CategoryStatus.INACTIVE);
            Competition competition = aCompetition();
            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
            when(repository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Category saved = service.save(category, 1L);

            assertThat(saved).isSameAs(category);
            assertThat(saved.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
            assertThat(saved.getCompetition()).isSameAs(competition);
            verify(repository).save(category);
        }

        @Test
        @DisplayName("should throw InvalidAgeException when the min age is greater than the max age")
        void shouldThrowInvalidAgeWhenMinGreaterThanMax() {
            Category category = Category.builder()
                    .name("Sub-15")
                    .minAge(15)
                    .maxAge(13)
                    .build();

            assertThatThrownBy(() -> service.save(category, 1L))
                    .isInstanceOf(InvalidAgeException.class)
                    .hasMessage("Min age cannot be greater than max age");
        }

        @Test
        @DisplayName("should throw CompetitionNotFoundException when the competition does not exist")
        void shouldThrowNotFoundWhenCompetitionDoesNotExist() {
            Category category = aCategory(CategoryStatus.ACTIVE);

            assertThatThrownBy(() -> service.save(category, 99L))
                    .isInstanceOf(CompetitionNotFoundException.class)
                    .hasMessage("Competition not found");
        }
    }

    @Nested
    @DisplayName("activate / deactivate")
    class ActivateDeactivate {

        @Test
        @DisplayName("should activate when INACTIVE")
        void shouldActivateWhenInactive() {
            Category category = aCategory(CategoryStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(category));

            Category result = service.activate(1L);

            assertThat(result.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
            verify(repository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("should keep ACTIVE when already ACTIVE (idempotent)")
        void shouldKeepActiveWhenAlreadyActive() {
            Category category = aCategory(CategoryStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(category));

            Category result = service.activate(1L);

            assertThat(result.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
            verify(repository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when activating a nonexistent id")
        void shouldThrowNotFoundWhenActivatingNonexistentId() {
            assertThatThrownBy(() -> service.activate(1L))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessage("Category not found");
        }

        @Test
        @DisplayName("should deactivate when ACTIVE")
        void shouldDeactivateWhenActive() {
            Category category = aCategory(CategoryStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(category));

            Category result = service.deactivate(1L);

            assertThat(result.getStatus()).isEqualTo(CategoryStatus.INACTIVE);
            verify(repository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("should keep INACTIVE when already INACTIVE (idempotent)")
        void shouldKeepInactiveWhenAlreadyInactive() {
            Category category = aCategory(CategoryStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(category));

            Category result = service.deactivate(1L);

            assertThat(result.getStatus()).isEqualTo(CategoryStatus.INACTIVE);
            verify(repository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when deactivating a nonexistent id")
        void shouldThrowNotFoundWhenDeactivatingNonexistentId() {
            assertThatThrownBy(() -> service.deactivate(1L))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessage("Category not found");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        private Category newData() {
            return Category.builder()
                    .name("Sub-17")
                    .minAge(15)
                    .maxAge(17)
                    .build();
        }

        @Test
        @DisplayName("should copy name and ages and not call save")
        void shouldUpdateDetailsWhenActive() {
            Category existing = aCategory(CategoryStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            Category result = service.update(1L, newData());

            assertThat(result).isSameAs(existing);
            assertThat(result.getName()).isEqualTo("Sub-17");
            assertThat(result.getMinAge()).isEqualTo(15);
            assertThat(result.getMaxAge()).isEqualTo(17);
            verify(repository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("should throw InactiveCategoryException when the category is INACTIVE")
        void shouldThrowInactiveWhenUpdatingInactive() {
            Category existing = aCategory(CategoryStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.update(1L, newData()))
                    .isInstanceOf(InactiveCategoryException.class)
                    .hasMessage("Category is inactive; reactivate it before editing");
        }

        @Test
        @DisplayName("should throw InvalidAgeException when the ages are invalid")
        void shouldThrowInvalidAgeWhenAgesAreInvalid() {
            Category existing = aCategory(CategoryStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            Category newData = Category.builder()
                    .name("Sub-17")
                    .minAge(17)
                    .maxAge(15)
                    .build();

            assertThatThrownBy(() -> service.update(1L, newData))
                    .isInstanceOf(InvalidAgeException.class)
                    .hasMessage("Min age cannot be greater than max age");
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when the id does not exist")
        void shouldThrowNotFoundWhenUpdatingNonexistentId() {
            assertThatThrownBy(() -> service.update(1L, newData()))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessage("Category not found");
        }
    }

    @Nested
    @DisplayName("business rules")
    class BusinessRules {

        private Competition competitionWithStatus(CompetitionStatus status) {
            return Competition.builder().id(1L).name("Copa").year(2026).status(status).build();
        }

        @Test
        @DisplayName("should block a duplicate age range on create")
        void shouldBlockDuplicateAgeRangeOnCreate() {
            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competitionWithStatus(CompetitionStatus.REGISTRATION_OPEN)));
            when(repository.existsByCompetitionIdAndMinAgeAndMaxAgeAndStatus(
                    1L, 13, 15, CategoryStatus.ACTIVE)).thenReturn(true);

            assertThatThrownBy(() -> service.save(aCategory(CategoryStatus.ACTIVE), 1L))
                    .isInstanceOf(DuplicateCategoryException.class);
        }

        @Test
        @DisplayName("should block a duplicate age range on update")
        void shouldBlockDuplicateAgeRangeOnUpdate() {
            Category existing = Category.builder().id(2L).name("Sub-15").minAge(13).maxAge(15)
                    .status(CategoryStatus.ACTIVE).competition(competitionWithStatus(CompetitionStatus.REGISTRATION_OPEN)).build();
            when(repository.findById(2L)).thenReturn(Optional.of(existing));
            when(repository.existsByCompetitionIdAndMinAgeAndMaxAgeAndStatusAndIdNot(
                    1L, 13, 15, CategoryStatus.ACTIVE, 2L)).thenReturn(true);

            assertThatThrownBy(() -> service.update(2L, aCategory(CategoryStatus.ACTIVE)))
                    .isInstanceOf(DuplicateCategoryException.class);
        }

        @Test
        @DisplayName("should block a duplicate age range on activate")
        void shouldBlockDuplicateAgeRangeOnActivate() {
            Category existing = Category.builder().id(2L).name("Sub-15").minAge(13).maxAge(15)
                    .status(CategoryStatus.INACTIVE).competition(competitionWithStatus(CompetitionStatus.REGISTRATION_OPEN)).build();
            when(repository.findById(2L)).thenReturn(Optional.of(existing));
            when(repository.existsByCompetitionIdAndMinAgeAndMaxAgeAndStatusAndIdNot(
                    1L, 13, 15, CategoryStatus.ACTIVE, 2L)).thenReturn(true);

            assertThatThrownBy(() -> service.activate(2L))
                    .isInstanceOf(DuplicateCategoryException.class);
        }

        @Test
        @DisplayName("should not deactivate a category with an active registration")
        void shouldNotDeactivateWithActiveRegistration() {
            Category existing = Category.builder().id(2L).status(CategoryStatus.ACTIVE).build();
            when(repository.findById(2L)).thenReturn(Optional.of(existing));
            when(teamRegistrationRepository.existsByCategoryIdAndStatusIn(2L, RegistrationStatus.OCCUPY_SLOT))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.deactivate(2L))
                    .isInstanceOf(ActiveReferenceException.class);
        }

        @Test
        @DisplayName("should block creation in a completed competition")
        void shouldBlockCreationInCompletedCompetition() {
            when(competitionRepository.findById(1L)).thenReturn(Optional.of(competitionWithStatus(CompetitionStatus.COMPLETED)));

            assertThatThrownBy(() -> service.save(aCategory(CategoryStatus.ACTIVE), 1L))
                    .isInstanceOf(CompetitionNotEditableException.class);
        }
    }
}
