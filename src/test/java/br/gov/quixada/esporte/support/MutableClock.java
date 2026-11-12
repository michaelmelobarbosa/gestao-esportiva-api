package br.gov.quixada.esporte.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link Clock} de teste que pode ser avançado manualmente, permitindo simular a
 * expiração de janelas de rate limit sem {@code Thread.sleep}.
 */
public final class MutableClock extends Clock {

    private final AtomicReference<Instant> agora;
    private final ZoneId zone;

    public MutableClock(Instant inicial, ZoneId zone) {
        this(new AtomicReference<>(inicial), zone);
    }

    private MutableClock(AtomicReference<Instant> agora, ZoneId zone) {
        this.agora = agora;
        this.zone = zone;
    }

    public void advance(Duration duracao) {
        agora.updateAndGet(instant -> instant.plus(duracao));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId novaZone) {
        return new MutableClock(agora, novaZone);
    }

    @Override
    public Instant instant() {
        return agora.get();
    }
}
