package br.gov.quixada.esporte.competicao;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class CompeticaoStatusScheduler {

    private final CompeticaoRepository repository;
    private final Clock clock;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void sincronizar() {
        var hoje = LocalDate.now(clock);
        repository.findAll().forEach(competicao -> competicao.sincronizarStatus(hoje));
    }
}
