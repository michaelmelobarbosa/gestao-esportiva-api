package br.gov.quixada.sports.competition;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class CompetitionStatusScheduler {

    private final CompetitionRepository repository;
    private final Clock clock;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void sincronizar() {
        var hoje = LocalDate.now(clock);
        repository.findAll().forEach(competition -> competition.syncStatus(hoje));
    }
}
