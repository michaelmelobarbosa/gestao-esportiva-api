package br.gov.quixada.sports.match;

import br.gov.quixada.sports.match.exception.InvalidMatchException;
import br.gov.quixada.sports.registration.TeamRegistration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class RoundRobinScheduler {

    public record ScheduledPairing(int round, TeamRegistration home, TeamRegistration away) {
    }

    public List<ScheduledPairing> generate(List<TeamRegistration> approvedRegistrations, boolean doubleRoundRobin) {
        if (approvedRegistrations == null) {
            throw new InvalidMatchException("At least 2 approved teams are required to generate a schedule");
        }

        List<TeamRegistration> sorted = approvedRegistrations.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        TeamRegistration::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        int n = sorted.size();
        if (n < 2) {
            throw new InvalidMatchException("At least 2 approved teams are required to generate a schedule");
        }

        List<TeamRegistration> slots = new ArrayList<>(sorted);
        if (n % 2 != 0) {
            slots.add(null);
        }

        int m = slots.size();
        int roundsPerLeg = m - 1;
        int half = m / 2;

        List<ScheduledPairing> firstLeg = new ArrayList<>();
        for (int round = 1; round <= roundsPerLeg; round++) {
            for (int i = 0; i < half; i++) {
                TeamRegistration first = slots.get(i);
                TeamRegistration second = slots.get(m - 1 - i);
                if (first != null && second != null) {
                    if (i == 0 && round % 2 == 0) {
                        firstLeg.add(new ScheduledPairing(round, second, first));
                    } else {
                        firstLeg.add(new ScheduledPairing(round, first, second));
                    }
                }
            }
            slots.add(1, slots.remove(m - 1));
        }

        if (!doubleRoundRobin) {
            return List.copyOf(firstLeg);
        }

        List<ScheduledPairing> allPairings = new ArrayList<>(firstLeg.size() * 2);
        allPairings.addAll(firstLeg);
        for (ScheduledPairing pairing : firstLeg) {
            allPairings.add(new ScheduledPairing(
                    pairing.round() + roundsPerLeg,
                    pairing.away(),
                    pairing.home()));
        }
        return List.copyOf(allPairings);
    }
}
