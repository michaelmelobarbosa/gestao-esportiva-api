package br.gov.quixada.sports.match;

import br.gov.quixada.sports.category.Category;
import br.gov.quixada.sports.category.CategoryRepository;
import br.gov.quixada.sports.category.exception.CategoryNotFoundException;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionRepository;
import br.gov.quixada.sports.competition.exception.CompetitionNotFoundException;
import br.gov.quixada.sports.registration.TeamRegistration;
import br.gov.quixada.sports.registration.TeamRegistrationRepository;
import br.gov.quixada.sports.registration.RegistrationStatus;
import br.gov.quixada.sports.registration.exception.InvalidRegistrationException;
import br.gov.quixada.sports.match.dto.StandingsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StandingsService {

    private final MatchRepository repository;
    private final CompetitionRepository competitionRepository;
    private final CategoryRepository categoryRepository;
    private final TeamRegistrationRepository teamRegistrationRepository;

    @Transactional(readOnly = true)
    public List<StandingsResponse> calculate(Long competitionId, Long categoryId) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new CompetitionNotFoundException("Competition not found"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));

        if (!category.getCompetition().getId().equals(competition.getId())) {
            throw new InvalidRegistrationException("Category does not belong to the informed competition");
        }

        Map<Long, Linha> linhas = new LinkedHashMap<>();
        teamRegistrationRepository.findByCompetitionIdAndCategoryIdAndStatus(
                        competitionId, categoryId, RegistrationStatus.APPROVED)
                .forEach(registration -> linhas.put(registration.getId(), new Linha(registration)));

        List<Match> matches = repository.findByCompetitionAndCategory(competitionId, categoryId);
        for (Match match : matches) {
            if (!matchValidForStandings(match)) {
                continue;
            }

            TeamRegistration home = match.getHomeRegistration();
            TeamRegistration away = match.getAwayRegistration();
            Linha homeLine = linhas.computeIfAbsent(home.getId(), id -> new Linha(home));
            Linha awayLine = linhas.computeIfAbsent(away.getId(), id -> new Linha(away));

            int homeGoals = match.getHomeGoals();
            int awayGoals = match.getAwayGoals();

            homeLine.played++;
            awayLine.played++;
            homeLine.goalsFor += homeGoals;
            homeLine.goalsAgainst += awayGoals;
            awayLine.goalsFor += awayGoals;
            awayLine.goalsAgainst += homeGoals;

            if (homeGoals > awayGoals) {
                homeLine.wins++;
                awayLine.losses++;
            } else if (homeGoals < awayGoals) {
                awayLine.wins++;
                homeLine.losses++;
            } else {
                homeLine.draws++;
                awayLine.draws++;
            }
        }

        List<Linha> ordenadas = new ArrayList<>(linhas.values());
        ordenadas.sort((a, b) -> comparar(a, b, matches));

        List<StandingsResponse> resposta = new ArrayList<>();
        for (int i = 0; i < ordenadas.size(); i++) {
            Linha linha = ordenadas.get(i);
            resposta.add(new StandingsResponse(
                    i + 1,
                    linha.registration.getId(),
                    linha.registration.getTeam().getId(),
                    linha.registration.getTeam().getName(),
                    linha.points(),
                    linha.played,
                    linha.wins,
                    linha.draws,
                    linha.losses,
                    linha.goalsFor,
                    linha.goalsAgainst,
                    linha.goalDifference(),
                    linha.pointsPercentage()
            ));
        }
        return resposta;
    }

    private boolean matchValidForStandings(Match match) {
        return match.getStatus() == MatchStatus.COMPLETED
                && match.getHomeGoals() != null
                && match.getAwayGoals() != null;
    }

    private int comparar(Linha a, Linha b, List<Match> matches) {
        int comparacao = Integer.compare(b.points(), a.points());
        if (comparacao != 0) return comparacao;

        comparacao = Integer.compare(b.wins, a.wins);
        if (comparacao != 0) return comparacao;

        comparacao = Integer.compare(b.goalDifference(), a.goalDifference());
        if (comparacao != 0) return comparacao;

        comparacao = Integer.compare(b.goalsFor, a.goalsFor);
        if (comparacao != 0) return comparacao;

        comparacao = confrontoDireto(a, b, matches);
        if (comparacao != 0) return comparacao;

        return a.registration.getTeam().getName().compareToIgnoreCase(b.registration.getTeam().getName());
    }

    private int confrontoDireto(Linha a, Linha b, List<Match> matches) {
        int pointsA = 0;
        int pointsB = 0;
        int differenceA = 0;
        int differenceB = 0;

        for (Match match : matches) {
            if (!matchValidForStandings(match)) continue;

            boolean aIsHome = match.getHomeRegistration().getId().equals(a.registration.getId());
            boolean bIsHome = match.getHomeRegistration().getId().equals(b.registration.getId());
            if (!(aIsHome && match.getAwayRegistration().getId().equals(b.registration.getId()))
                    && !(bIsHome && match.getAwayRegistration().getId().equals(a.registration.getId()))) {
                continue;
            }

            int goalsA = aIsHome ? match.getHomeGoals() : match.getAwayGoals();
            int goalsB = aIsHome ? match.getAwayGoals() : match.getHomeGoals();
            differenceA += goalsA - goalsB;
            differenceB += goalsB - goalsA;

            if (goalsA > goalsB) {
                pointsA += 3;
            } else if (goalsB > goalsA) {
                pointsB += 3;
            } else {
                pointsA++;
                pointsB++;
            }
        }

        int comparacao = Integer.compare(pointsB, pointsA);
        if (comparacao != 0) return comparacao;
        return Integer.compare(differenceB, differenceA);
    }

    private static final class Linha {
        private final TeamRegistration registration;
        private int played;
        private int wins;
        private int draws;
        private int losses;
        private int goalsFor;
        private int goalsAgainst;

        private Linha(TeamRegistration registration) {
            this.registration = registration;
        }

        private int points() {
            return wins * 3 + draws;
        }

        private int goalDifference() {
            return goalsFor - goalsAgainst;
        }

        private double pointsPercentage() {
            if (played == 0) return 0.0;
            double valor = (points() / (played * 3.0)) * 100.0;
            return Math.round(valor * 100.0) / 100.0;
        }
    }
}
