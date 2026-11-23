package br.gov.quixada.sports.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;

import br.gov.quixada.sports.match.RoundRobinScheduler.ScheduledPairing;
import br.gov.quixada.sports.match.exception.InvalidMatchException;
import br.gov.quixada.sports.registration.RegistrationStatus;
import br.gov.quixada.sports.registration.TeamRegistration;
import br.gov.quixada.sports.team.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RoundRobinScheduler")
class RoundRobinSchedulerTest {

    private final RoundRobinScheduler scheduler = new RoundRobinScheduler();

    private TeamRegistration registration(Long id, String name) {
        return TeamRegistration.builder()
                .id(id)
                .status(RegistrationStatus.APPROVED)
                .team(Team.builder().id(id).name(name).build())
                .build();
    }

    private List<TeamRegistration> registrations(int count) {
        return LongStream.rangeClosed(1, count)
                .mapToObj(id -> registration(id, "Team " + id))
                .toList();
    }

    private void assertRoundRobinInvariants(List<TeamRegistration> teams,
                                            List<ScheduledPairing> pairings,
                                            boolean doubleRoundRobin) {
        int n = teams.size();
        int roundsPerLeg = (n % 2 == 0) ? n - 1 : n;
        int expectedRounds = doubleRoundRobin ? roundsPerLeg * 2 : roundsPerLeg;
        int expectedMatchesPerLeg = n * (n - 1) / 2;
        int expectedMatches = doubleRoundRobin ? expectedMatchesPerLeg * 2 : expectedMatchesPerLeg;

        assertThat(pairings).hasSize(expectedMatches);
        assertThat(pairings.stream().mapToInt(ScheduledPairing::round).max().orElse(0))
                .isEqualTo(expectedRounds);

        Map<Integer, Set<Long>> teamsPerRound = new HashMap<>();
        Map<Set<Long>, Integer> unorderedPairCount = new HashMap<>();
        Set<String> orderedPairs = new HashSet<>();

        for (ScheduledPairing pairing : pairings) {
            assertThat(pairing.round()).isBetween(1, expectedRounds);
            assertThat(pairing.home()).isNotNull();
            assertThat(pairing.away()).isNotNull();
            assertThat(pairing.home().getId()).isNotEqualTo(pairing.away().getId());

            Set<Long> roundTeams = teamsPerRound.computeIfAbsent(pairing.round(), r -> new HashSet<>());
            assertThat(roundTeams.add(pairing.home().getId()))
                    .as("Team %d already plays in round %d", pairing.home().getId(), pairing.round())
                    .isTrue();
            assertThat(roundTeams.add(pairing.away().getId()))
                    .as("Team %d already plays in round %d", pairing.away().getId(), pairing.round())
                    .isTrue();

            Set<Long> unorderedPair = Set.of(pairing.home().getId(), pairing.away().getId());
            unorderedPairCount.merge(unorderedPair, 1, Integer::sum);

            String directedKey = pairing.home().getId() + "->" + pairing.away().getId();
            assertThat(orderedPairs.add(directedKey))
                    .as("Duplicate directed pairing %s", directedKey)
                    .isTrue();
        }

        int expectedMeetsPerPair = doubleRoundRobin ? 2 : 1;
        assertThat(unorderedPairCount).hasSize(expectedMatchesPerLeg);
        assertThat(unorderedPairCount.values()).allMatch(count -> count == expectedMeetsPerPair);
    }

    @Test
    @DisplayName("should reject null or fewer than 2 teams")
    void shouldRejectFewerThanTwoTeams() {
        assertThatThrownBy(() -> scheduler.generate(null, false))
                .isInstanceOf(InvalidMatchException.class)
                .hasMessage("At least 2 approved teams are required to generate a schedule");

        assertThatThrownBy(() -> scheduler.generate(List.of(), false))
                .isInstanceOf(InvalidMatchException.class)
                .hasMessage("At least 2 approved teams are required to generate a schedule");

        assertThatThrownBy(() -> scheduler.generate(registrations(1), true))
                .isInstanceOf(InvalidMatchException.class)
                .hasMessage("At least 2 approved teams are required to generate a schedule");
    }

    @Test
    @DisplayName("should generate 1 round and 1 match for 2 teams in single round-robin")
    void shouldGenerateForTwoTeams() {
        var teams = registrations(2);

        var pairings = scheduler.generate(teams, false);

        assertRoundRobinInvariants(teams, pairings, false);
        assertThat(pairings).hasSize(1);
        assertThat(pairings.getFirst().round()).isEqualTo(1);
        assertThat(pairings.getFirst().home().getId()).isEqualTo(1L);
        assertThat(pairings.getFirst().away().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("should generate 3 rounds and 6 matches with balanced home/away for 4 teams")
    void shouldGenerateForFourTeams() {
        var teams = registrations(4);

        var pairings = scheduler.generate(teams, false);

        assertRoundRobinInvariants(teams, pairings, false);

        Map<Long, Integer> homeCounts = new HashMap<>();
        for (ScheduledPairing pairing : pairings) {
            homeCounts.merge(pairing.home().getId(), 1, Integer::sum);
        }
        for (TeamRegistration team : teams) {
            assertThat(homeCounts.getOrDefault(team.getId(), 0)).isBetween(1, 2);
        }
    }

    @Test
    @DisplayName("should generate 5 rounds and 10 matches handling bye slot for 5 odd teams")
    void shouldGenerateForFiveOddTeams() {
        var teams = registrations(5);

        var pairings = scheduler.generate(teams, false);

        assertRoundRobinInvariants(teams, pairings, false);
        for (int round = 1; round <= 5; round++) {
            int r = round;
            assertThat(pairings.stream().filter(p -> p.round() == r).count()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("should mirror second leg with inverted home and away in double round-robin")
    void shouldGenerateDoubleRoundRobinWithInvertedSecondLeg() {
        var teams = registrations(4);

        var pairings = scheduler.generate(teams, true);

        assertRoundRobinInvariants(teams, pairings, true);

        List<ScheduledPairing> firstLeg = pairings.stream().filter(p -> p.round() <= 3).toList();
        List<ScheduledPairing> secondLeg = pairings.stream().filter(p -> p.round() > 3).toList();
        assertThat(secondLeg).hasSameSizeAs(firstLeg);

        for (int i = 0; i < firstLeg.size(); i++) {
            ScheduledPairing leg1 = firstLeg.get(i);
            ScheduledPairing leg2 = secondLeg.get(i);
            assertThat(leg2.round()).isEqualTo(leg1.round() + 3);
            assertThat(leg2.home().getId()).isEqualTo(leg1.away().getId());
            assertThat(leg2.away().getId()).isEqualTo(leg1.home().getId());
        }
    }

    @Test
    @DisplayName("should sort input deterministically by registration id and handle null ids safely")
    void shouldSortDeterministicallyAndHandleNullIds() {
        var t1 = registration(10L, "Team 10");
        var t2 = registration(20L, "Team 20");
        var t3 = registration(30L, "Team 30");
        var tNull = registration(null, "Team Null");

        var forward = scheduler.generate(List.of(t1, t2, t3), false);
        var shuffled = scheduler.generate(List.of(t3, t1, t2), false);

        assertThat(shuffled).isEqualTo(forward);

        var withNullId = scheduler.generate(List.of(tNull, t2, t1), false);
        assertThat(withNullId).hasSize(3);
    }
}
