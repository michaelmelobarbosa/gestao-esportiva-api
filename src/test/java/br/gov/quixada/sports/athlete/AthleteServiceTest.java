package br.gov.quixada.sports.athlete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import br.gov.quixada.sports.athlete.exception.InactiveAthleteException;
import br.gov.quixada.sports.athlete.exception.AthleteNotDeletableException;
import br.gov.quixada.sports.athlete.exception.AthleteNotFoundException;
import br.gov.quixada.sports.athlete.exception.CpfAlreadyRegisteredException;
import br.gov.quixada.sports.common.error.ActiveReferenceException;
import br.gov.quixada.sports.registration.RegistrationStatus;
import br.gov.quixada.sports.shared.Address;
import br.gov.quixada.sports.shared.Gender;
import br.gov.quixada.sports.registration.AthleteRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AthleteService")
class AthleteServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private AthleteRepository repository;

    @Mock
    private AthleteRegistrationRepository athleteRegistrationRepository;

    private AthleteService service;

    @BeforeEach
    void setUp() {
        service = new AthleteService(repository, athleteRegistrationRepository);
    }

    private Athlete anAthlete(AthleteStatus status) {
        return Athlete.builder()
                .fullName("Francisco")
                .cpf("52998224725")
                .birthDate(LocalDate.of(2000, 1, 1))
                .address(new Address("A Street", "10", "Downtown", "Quixadá", "63900-000"))
                .phone("88999999999")
                .gender(Gender.MALE)
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @Order(1)
        @DisplayName("should find all when name is null")
        void shouldFindAllWhenNameIsNull() {
            Page<Athlete> expected = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(expected);

            Page<Athlete> result = service.findAll(null, null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNormalizedFullNameStartingWith(anyString(), any(Pageable.class));
        }

        @Test
        @Order(2)
        @DisplayName("should find all when name is blank")
        void shouldFindAllWhenNameIsBlank() {
            Page<Athlete> expected = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(expected);

            Page<Athlete> result = service.findAll("   ", null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findAll(PAGEABLE);
            verify(repository, never()).findByNormalizedFullNameStartingWith(anyString(), any(Pageable.class));
        }

        @Test
        @Order(3)
        @DisplayName("should filter by normalized name when provided")
        void shouldFilterByNormalizedNameWhenProvided() {
            Page<Athlete> expected = new PageImpl<>(List.of(anAthlete(AthleteStatus.ACTIVE)));
            when(repository.findByNormalizedFullNameStartingWith("joao", PAGEABLE)).thenReturn(expected);

            Page<Athlete> result = service.findAll("joao", null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findByNormalizedFullNameStartingWith("joao", PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }

        @Test
        @Order(4)
        @DisplayName("should normalize the search input (accent, case and spacing)")
        void shouldNormalizeSearchInput() {
            Page<Athlete> expected = Page.empty();
            when(repository.findByNormalizedFullNameStartingWith("joaosilva", PAGEABLE)).thenReturn(expected);

            Page<Athlete> result = service.findAll("João Silva", null, PAGEABLE);

            assertThat(result).isSameAs(expected);
            verify(repository).findByNormalizedFullNameStartingWith("joaosilva", PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @Order(4)
        @DisplayName("should return the athlete when it exists")
        void shouldReturnAthleteWhenItExists() {
            Athlete athlete = anAthlete(AthleteStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(athlete));

            Athlete result = service.findByIdOrThrowNotFound(1L);

            assertThat(result).isSameAs(athlete);
            verify(repository).findById(1L);
        }

        @Test
        @Order(5)
        @DisplayName("should throw AthleteNotFoundException when it does not exist")
        void shouldThrowNotFoundWhenItDoesNotExist() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(AthleteNotFoundException.class)
                    .hasMessage("Athlete not found");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @Order(6)
        @DisplayName("should normalize the CPF and force ACTIVE status")
        void shouldNormalizeCpfAndForceActiveStatus() {
            Athlete athlete = Athlete.builder()
                    .fullName("John de Tal")
                    .cpf("529.982.247-25")
                    .birthDate(LocalDate.of(2000, 1, 1))
                    .address(new Address("A Street", "10", "Downtown", "Quixadá", "63900-000"))
                    .phone("88999999999")
                    .gender(Gender.MALE)
                    .status(AthleteStatus.INACTIVE)
                    .build();
            when(repository.save(any(Athlete.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Athlete saved = service.save(athlete);

            assertThat(saved).isSameAs(athlete);
            assertThat(saved.getCpf()).isEqualTo("52998224725");
            assertThat(saved.getStatus()).isEqualTo(AthleteStatus.ACTIVE);
            verify(repository).save(athlete);
        }

        @Test
        @Order(7)
        @DisplayName("should throw CpfAlreadyRegisteredException when the error is 1062")
        void shouldThrowCpfAlreadyRegisteredWhenErrorIs1062() {
            Athlete athlete = anAthlete(AthleteStatus.ACTIVE);
            SQLIntegrityConstraintViolationException cause =
                    new SQLIntegrityConstraintViolationException("Duplicate entry", "23000", 1062);
            when(repository.save(any(Athlete.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate", cause));

            assertThatThrownBy(() -> service.save(athlete))
                    .isInstanceOf(CpfAlreadyRegisteredException.class)
                    .hasMessage("CPF already registered");
        }

        @Test
        @Order(8)
        @DisplayName("should rethrow when the MySQL code is not 1062")
        void shouldRethrowWhenMysqlCodeIsNot1062() {
            Athlete athlete = anAthlete(AthleteStatus.ACTIVE);
            SQLIntegrityConstraintViolationException cause =
                    new SQLIntegrityConstraintViolationException("Foreign key constraint fails", "2300", 1452);
            DataIntegrityViolationException exception =
                    new DataIntegrityViolationException("conflito", cause);
            when(repository.save(any(Athlete.class))).thenThrow(exception);

            assertThatThrownBy(() -> service.save(athlete))
                    .isSameAs(exception);
        }

        @Test
        @Order(9)
        @DisplayName("should rethrow when the cause is not an SQLIntegrityConstraintViolationException")
        void shouldRethrowWhenCauseIsNotSqlException() {
            Athlete athlete = anAthlete(AthleteStatus.ACTIVE);
            DataIntegrityViolationException exception =
                    new DataIntegrityViolationException("conflito", new RuntimeException("outro erro"));
            when(repository.save(any(Athlete.class))).thenThrow(exception);

            assertThatThrownBy(() -> service.save(athlete))
                    .isSameAs(exception);
        }
    }

    @Nested
    @DisplayName("activate / deactivate")
    class ActivateDeactivate {

        @Test
        @Order(10)
        @DisplayName("should activate when INACTIVE")
        void shouldActivateWhenInactive() {
            Athlete athlete = anAthlete(AthleteStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(athlete));

            Athlete result = service.activate(1L);

            assertThat(result.getStatus()).isEqualTo(AthleteStatus.ACTIVE);
            verify(repository, never()).save(any(Athlete.class));
        }

        @Test
        @Order(11)
        @DisplayName("should keep ACTIVE when already ACTIVE (idempotent)")
        void shouldKeepActiveWhenAlreadyActive() {
            Athlete athlete = anAthlete(AthleteStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(athlete));

            Athlete result = service.activate(1L);

            assertThat(result.getStatus()).isEqualTo(AthleteStatus.ACTIVE);
            verify(repository, never()).save(any(Athlete.class));
        }

        @Test
        @Order(12)
        @DisplayName("should throw AthleteNotFoundException when activating a nonexistent id")
        void shouldThrowNotFoundWhenActivatingNonexistentId() {
            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.activate(1L))
                    .isInstanceOf(AthleteNotFoundException.class);
        }

        @Test
        @Order(13)
        @DisplayName("should deactivate when ACTIVE")
        void shouldDeactivateWhenActive() {
            Athlete athlete = anAthlete(AthleteStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(athlete));

            Athlete result = service.deactivate(1L);

            assertThat(result.getStatus()).isEqualTo(AthleteStatus.INACTIVE);
            verify(repository, never()).save(any(Athlete.class));
        }

        @Test
        @Order(14)
        @DisplayName("should keep INACTIVE when already INACTIVE (idempotent)")
        void shouldKeepInactiveWhenAlreadyInactive() {
            Athlete athlete = anAthlete(AthleteStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(athlete));

            Athlete result = service.deactivate(1L);

            assertThat(result.getStatus()).isEqualTo(AthleteStatus.INACTIVE);
            verify(repository, never()).save(any(Athlete.class));
        }

        @Test
        @Order(15)
        @DisplayName("should throw AthleteNotFoundException when deactivating a nonexistent id")
        void shouldThrowNotFoundWhenDeactivatingNonexistentId() {
            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deactivate(1L))
                    .isInstanceOf(AthleteNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        private Athlete newData() {
            return Athlete.builder()
                    .fullName("Novo Name")
                    .cpf("11111111111")
                    .birthDate(LocalDate.of(1990, 5, 5))
                    .address(new Address("B Street", "20", "Neighborhood Novo", "Fortaleza", "60000-000"))
                    .phone("88888888888")
                    .gender(Gender.FEMALE)
                    .build();
        }

        @Test
        @Order(16)
        @DisplayName("should copy the data and return the same instance without saving")
        void shouldUpdateDetailsWhenActive() {
            Athlete existing = anAthlete(AthleteStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            Athlete result = service.update(1L, newData());

            assertThat(result).isSameAs(existing);
            assertThat(result.getFullName()).isEqualTo("Novo Name");
            assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 5));
            assertThat(result.getAddress().getCity()).isEqualTo("Fortaleza");
            assertThat(result.getPhone()).isEqualTo("88888888888");
            assertThat(result.getGender()).isEqualTo(Gender.FEMALE);
            verify(repository, never()).save(any(Athlete.class));
        }

        @Test
        @Order(17)
        @DisplayName("should not change the CPF (immutable after creation)")
        void shouldNotChangeCpf() {
            Athlete existing = anAthlete(AthleteStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            Athlete result = service.update(1L, newData());

            assertThat(result.getCpf()).isEqualTo("52998224725");
        }

        @Test
        @Order(18)
        @DisplayName("should overwrite fields with nulls (full replacement; DTO @Valid prevents this via the API)")
        void shouldOverwriteFieldsWithNulls() {
            Athlete existing = anAthlete(AthleteStatus.ACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            Athlete result = service.update(1L, Athlete.builder().build());

            assertThat(result).isSameAs(existing);
            assertThat(result.getFullName()).isNull();
            assertThat(result.getBirthDate()).isNull();
            assertThat(result.getAddress()).isNull();
            assertThat(result.getPhone()).isNull();
            assertThat(result.getGender()).isNull();
            assertThat(result.getCpf()).isEqualTo("52998224725");
        }

        @Test
        @Order(19)
        @DisplayName("should throw InactiveAthleteException when the athlete is INACTIVE")
        void shouldThrowInactiveWhenUpdatingInactive() {
            Athlete existing = anAthlete(AthleteStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.update(1L, newData()))
                    .isInstanceOf(InactiveAthleteException.class)
                    .hasMessage("Athlete is inactive; reactivate it before editing");
        }

        @Test
        @Order(20)
        @DisplayName("should throw AthleteNotFoundException when the id does not exist")
        void shouldThrowNotFoundWhenUpdatingNonexistentId() {
            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(1L, newData()))
                    .isInstanceOf(AthleteNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("business rules")
    class BusinessRules {

        @Test
        @DisplayName("should find athlete by normalized CPF")
        void shouldFindByNormalizedCpf() {
            Athlete athlete = anAthlete(AthleteStatus.ACTIVE);
            when(repository.findByCpf("52998224725")).thenReturn(Optional.of(athlete));

            var result = service.findByCpfOrThrowNotFound("529.982.247-25");

            assertThat(result).isSameAs(athlete);
        }

        @Test
        @DisplayName("should throw AthleteNotFoundException when the CPF does not exist")
        void shouldThrowNotFoundWhenCpfDoesNotExist() {
            when(repository.findByCpf("52998224725")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByCpfOrThrowNotFound("52998224725"))
                    .isInstanceOf(AthleteNotFoundException.class);
        }

        @Test
        @DisplayName("should not deactivate an athlete with an active registration")
        void shouldNotDeactivateWithActiveRegistration() {
            Athlete athlete = anAthlete(AthleteStatus.ACTIVE);
            athlete = Athlete.builder()
                    .id(1L)
                    .fullName("Francisco")
                    .cpf("52998224725")
                    .birthDate(LocalDate.of(2000, 1, 1))
                    .address(new Address("A Street", "10", "Downtown", "Quixadá", "63900-000"))
                    .phone("88999999999")
                    .gender(Gender.MALE)
                    .status(AthleteStatus.ACTIVE)
                    .build();
            when(repository.findById(1L)).thenReturn(Optional.of(athlete));
            when(athleteRegistrationRepository.existsByAthleteIdAndStatusIn(1L, RegistrationStatus.OCCUPY_SLOT))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.deactivate(1L))
                    .isInstanceOf(ActiveReferenceException.class);
        }
    }

    @Nested
    @DisplayName("hardDelete")
    class HardDelete {

        private Athlete athleteWithId(Long id, AthleteStatus status) {
            return Athlete.builder()
                    .id(id)
                    .fullName("Francisco")
                    .cpf("52998224725")
                    .birthDate(LocalDate.of(2000, 1, 1))
                    .address(new Address("A Street", "10", "Downtown", "Quixadá", "63900-000"))
                    .phone("88999999999")
                    .gender(Gender.MALE)
                    .status(status)
                    .build();
        }

        @Test
        @DisplayName("should refuse to delete an active athlete")
        void shouldNotDeleteActiveAthlete() {
            when(repository.findById(1L)).thenReturn(Optional.of(athleteWithId(1L, AthleteStatus.ACTIVE)));

            assertThatThrownBy(() -> service.hardDelete(1L))
                    .isInstanceOf(AthleteNotDeletableException.class)
                    .hasMessageContaining("deactivate");

            verify(repository, never()).delete(any(Athlete.class));
            verify(athleteRegistrationRepository, never()).deleteByAthleteIdAndStatusIn(any(), anyCollection());
        }

        @Test
        @DisplayName("should refuse to delete when there is a registration that occupies a slot")
        void shouldNotDeleteWhenRegistrationOccupiesSlot() {
            Athlete athlete = athleteWithId(1L, AthleteStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(athlete));
            when(athleteRegistrationRepository.existsByAthleteIdAndStatusIn(1L, RegistrationStatus.OCCUPY_SLOT))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.hardDelete(1L))
                    .isInstanceOf(AthleteNotDeletableException.class);

            verify(repository, never()).delete(any(Athlete.class));
            verify(athleteRegistrationRepository, never()).deleteByAthleteIdAndStatusIn(any(), anyCollection());
        }

        @Test
        @DisplayName("should delete registrations that free the slot and then the athlete")
        void shouldDeleteFreedRegistrationsThenTheAthlete() {
            Athlete athlete = athleteWithId(1L, AthleteStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(athlete));
            when(athleteRegistrationRepository.existsByAthleteIdAndStatusIn(1L, RegistrationStatus.OCCUPY_SLOT))
                    .thenReturn(false);

            service.hardDelete(1L);

            verify(athleteRegistrationRepository).deleteByAthleteIdAndStatusIn(1L, RegistrationStatus.FREE_SLOT);
            verify(repository).delete(athlete);
        }

        @Test
        @DisplayName("should delete an inactive athlete with no registrations")
        void shouldDeleteInactiveAthleteWithoutRegistrations() {
            Athlete athlete = athleteWithId(2L, AthleteStatus.INACTIVE);
            when(repository.findById(2L)).thenReturn(Optional.of(athlete));
            when(athleteRegistrationRepository.existsByAthleteIdAndStatusIn(2L, RegistrationStatus.OCCUPY_SLOT))
                    .thenReturn(false);

            service.hardDelete(2L);

            verify(repository).delete(athlete);
        }

        @Test
        @DisplayName("should throw not found when the athlete does not exist")
        void shouldThrowNotFoundWhenAthleteDoesNotExist() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.hardDelete(99L))
                    .isInstanceOf(AthleteNotFoundException.class);
        }
    }
}
