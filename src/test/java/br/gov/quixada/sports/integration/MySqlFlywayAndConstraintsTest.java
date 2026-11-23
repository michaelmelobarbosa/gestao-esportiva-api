package br.gov.quixada.sports.integration;

import br.gov.quixada.sports.athlete.Athlete;
import br.gov.quixada.sports.athlete.AthleteRepository;
import br.gov.quixada.sports.athlete.AthleteService;
import br.gov.quixada.sports.athlete.AthleteStatus;
import br.gov.quixada.sports.athlete.exception.CpfAlreadyRegisteredException;
import br.gov.quixada.sports.category.Category;
import br.gov.quixada.sports.category.CategoryRepository;
import br.gov.quixada.sports.category.CategoryStatus;
import br.gov.quixada.sports.club.Club;
import br.gov.quixada.sports.club.ClubRepository;
import br.gov.quixada.sports.club.ClubStatus;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionRepository;
import br.gov.quixada.sports.competition.CompetitionStatus;
import br.gov.quixada.sports.modality.Modality;
import br.gov.quixada.sports.modality.ModalityRepository;
import br.gov.quixada.sports.modality.ModalityStatus;
import br.gov.quixada.sports.registration.AthleteRegistration;
import br.gov.quixada.sports.registration.AthleteRegistrationRepository;
import br.gov.quixada.sports.registration.AthleteRegistrationService;
import br.gov.quixada.sports.registration.RegistrationStatus;
import br.gov.quixada.sports.registration.TeamRegistration;
import br.gov.quixada.sports.registration.TeamRegistrationRepository;
import br.gov.quixada.sports.registration.exception.AthleteAlreadyRegisteredException;
import br.gov.quixada.sports.registration.exception.JerseyNumberAlreadyUsedException;
import br.gov.quixada.sports.security.auth.AuthService;
import br.gov.quixada.sports.security.auth.UserRepository;
import br.gov.quixada.sports.security.auth.exception.EmailAlreadyRegisteredException;
import br.gov.quixada.sports.security.auth.exception.InvalidRefreshTokenException;
import br.gov.quixada.sports.security.jwt.RefreshTokenService;
import br.gov.quixada.sports.security.users.User;
import br.gov.quixada.sports.security.users.UserRole;
import br.gov.quixada.sports.security.users.UserStatus;
import br.gov.quixada.sports.security.users.dto.RegisterRequest;
import br.gov.quixada.sports.shared.Address;
import br.gov.quixada.sports.shared.Gender;
import br.gov.quixada.sports.support.AbstractMySqlIntegrationTest;
import br.gov.quixada.sports.team.Team;
import br.gov.quixada.sports.team.TeamRepository;
import br.gov.quixada.sports.team.TeamStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MySQL 8.4 + Flyway integration tests and schema constraints")
public class MySqlFlywayAndConstraintsTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private AthleteService athleteService;

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private ModalityRepository modalityRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamRegistrationRepository teamRegistrationRepository;

    @Autowired
    private AthleteRegistrationService athleteRegistrationService;

    @Autowired
    private AthleteRegistrationRepository athleteRegistrationRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("1. Flyway applied migrations cleanly and seeded superadmin exists with tokenVersion == 0")
    void flywayAndSchemaValidationSucceeded() {
        User superadmin = userRepository.findByEmail("admin@quixada.gov.br")
                .orElseThrow(() -> new AssertionError("Seeded superadmin not found"));

        assertThat(superadmin.getRole()).isEqualTo(UserRole.SUPERADMIN);
        assertThat(superadmin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(superadmin.getTokenVersion()).isEqualTo(0);
    }

    @Test
    @DisplayName("2. Duplicate CPF throws CpfAlreadyRegisteredException on real MySQL (error 1062)")
    void duplicateCpfThrowsCpfAlreadyRegisteredExceptionOnRealMySql() {
        String duplicateCpf = "52998224725";
        Athlete athlete1 = buildAthlete("Carlos Silva", duplicateCpf);
        athleteService.save(athlete1);

        Athlete athlete2 = buildAthlete("Outro Atleta", duplicateCpf);
        assertThatThrownBy(() -> athleteService.save(athlete2))
                .isInstanceOf(CpfAlreadyRegisteredException.class)
                .hasMessage("CPF already registered");
    }

    @Test
    @DisplayName("3. V23 conditional unique constraints allow reuse after cancellation or rejection")
    void v23ConditionalUniqueConstraintsAllowReuseAfterCancellationOrRejection() {
        Modality modality = createPersistedModality("Futebol V23");
        Competition competition = createPersistedCompetition("Copa Quixadá V23", modality);
        Category category = createPersistedCategory("Categoria Principal", competition);
        Club club = createPersistedClub("Clube Quixadá V23");
        Team team = createPersistedTeam("Canarinho V23", club, modality);
        TeamRegistration teamRegistration = createPersistedTeamRegistration(competition, category, team);

        // Keep 1 baseline active athlete on the APPROVED team registration so athlete A can be canceled
        // without violating the rule that an approved team must keep at least 1 active athlete.
        Athlete athleteKeeper = athleteRepository.save(buildAthlete("Goleiro Titular", "99988877714"));
        athleteRegistrationService.save(
                AthleteRegistration.builder().jerseyNumber("99").build(),
                teamRegistration.getId(),
                athleteKeeper.getId()
        );

        Athlete athleteA = athleteRepository.save(buildAthlete("Atleta A", "11144477735"));
        Athlete athleteB = athleteRepository.save(buildAthlete("Atleta B", "22233344405"));

        // Registers athlete A on teamRegistration with jersey "10" -> PENDING
        AthleteRegistration regA = athleteRegistrationService.save(
                AthleteRegistration.builder().jerseyNumber("10").build(),
                teamRegistration.getId(),
                athleteA.getId()
        );
        assertThat(regA.getStatus()).isEqualTo(RegistrationStatus.PENDING);
        assertThat(regA.getJerseyNumber()).isEqualTo("10");

        // Verifies a second registration with jersey "10" (for athlete B) throws JerseyNumberAlreadyUsedException
        assertThatThrownBy(() -> athleteRegistrationService.save(
                AthleteRegistration.builder().jerseyNumber("10").build(),
                teamRegistration.getId(),
                athleteB.getId()
        )).isInstanceOf(JerseyNumberAlreadyUsedException.class);

        // And a second active registration for athlete A (different jersey "11") throws AthleteAlreadyRegisteredException
        assertThatThrownBy(() -> athleteRegistrationService.save(
                AthleteRegistration.builder().jerseyNumber("11").build(),
                teamRegistration.getId(),
                athleteA.getId()
        )).isInstanceOf(AthleteAlreadyRegisteredException.class);

        // Cancels (or rejects) athlete A's registration -> CANCELED (FREE_SLOT)
        AthleteRegistration canceledA = athleteRegistrationService.changeStatus(regA.getId(), RegistrationStatus.CANCELED);
        assertThat(canceledA.getStatus()).isEqualTo(RegistrationStatus.CANCELED);

        // Now registers athlete B with jersey "10" -> succeeds in real MySQL!
        AthleteRegistration regB = athleteRegistrationService.save(
                AthleteRegistration.builder().jerseyNumber("10").build(),
                teamRegistration.getId(),
                athleteB.getId()
        );
        assertThat(regB.getStatus()).isEqualTo(RegistrationStatus.PENDING);
        assertThat(regB.getJerseyNumber()).isEqualTo("10");

        // And registers athlete A again with jersey "12" -> succeeds in real MySQL!
        AthleteRegistration regA2 = athleteRegistrationService.save(
                AthleteRegistration.builder().jerseyNumber("12").build(),
                teamRegistration.getId(),
                athleteA.getId()
        );
        assertThat(regA2.getStatus()).isEqualTo(RegistrationStatus.PENDING);
        assertThat(regA2.getJerseyNumber()).isEqualTo("12");
    }

    @Test
    @DisplayName("4. Users email and refresh token hashes are unique in MySQL and rotation works cleanly")
    void usersEmailAndRefreshTokenHashesAreUniqueInMySql() {
        var request = new RegisterRequest("testuser", "testuser@quixada.gov.br", "password123");
        authService.register(request);

        var duplicateRequest = new RegisterRequest("anotheruser", "testuser@quixada.gov.br", "password456");
        assertThatThrownBy(() -> authService.register(duplicateRequest))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        User user = userRepository.findByEmail("testuser@quixada.gov.br")
                .orElseThrow(() -> new AssertionError("Registered user not found"));
        String rawToken = refreshTokenService.issue(user);
        assertThat(rawToken).isNotBlank();

        RefreshTokenService.RotatedTokens rotated = refreshTokenService.rotate(rawToken);
        assertThat(rotated.refreshToken()).isNotBlank();
        assertThat(rotated.refreshToken()).isNotEqualTo(rawToken);
        assertThat(rotated.user().getId()).isEqualTo(user.getId());

        // Reusing the rotated (revoked) token fails
        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    private Athlete buildAthlete(String fullName, String cpf) {
        return Athlete.builder()
                .fullName(fullName)
                .cpf(cpf)
                .birthDate(LocalDate.now().minusYears(20))
                .address(new Address("Rua Teste", "100", "Centro", "Quixadá", "63900-000"))
                .phone("88999999999")
                .gender(Gender.MALE)
                .status(AthleteStatus.ACTIVE)
                .build();
    }

    private Modality createPersistedModality(String name) {
        Modality modality = Modality.builder()
                .name(name)
                .status(ModalityStatus.ACTIVE)
                .build();
        return modalityRepository.save(modality);
    }

    private Competition createPersistedCompetition(String name, Modality modality) {
        LocalDate today = LocalDate.now();
        Competition competition = Competition.builder()
                .name(name)
                .year(today.getYear())
                .modality(modality)
                .registrationStartDate(today.minusDays(1))
                .registrationEndDate(today.plusDays(5))
                .startDate(today.plusDays(6))
                .endDate(today.plusDays(20))
                .status(CompetitionStatus.REGISTRATION_OPEN)
                .build();
        return competitionRepository.save(competition);
    }

    private Category createPersistedCategory(String name, Competition competition) {
        Category category = Category.builder()
                .name(name)
                .minAge(15)
                .maxAge(35)
                .minAthletes(1)
                .maxAthletes(25)
                .competition(competition)
                .status(CategoryStatus.ACTIVE)
                .build();
        return categoryRepository.save(category);
    }

    private Club createPersistedClub(String name) {
        Club club = Club.builder()
                .name(name)
                .manager("Diretor Teste")
                .phone("88999999999")
                .address(new Address("Rua Clube", "200", "Centro", "Quixadá", "63900-000"))
                .status(ClubStatus.ACTIVE)
                .build();
        return clubRepository.save(club);
    }

    private Team createPersistedTeam(String name, Club club, Modality modality) {
        Team team = Team.builder()
                .name(name)
                .club(club)
                .modality(modality)
                .manager("Treinador Teste")
                .status(TeamStatus.ACTIVE)
                .build();
        return teamRepository.save(team);
    }

    private TeamRegistration createPersistedTeamRegistration(Competition competition, Category category, Team team) {
        TeamRegistration teamRegistration = TeamRegistration.builder()
                .competition(competition)
                .category(category)
                .team(team)
                .status(RegistrationStatus.APPROVED)
                .build();
        return teamRegistrationRepository.save(teamRegistration);
    }
}
