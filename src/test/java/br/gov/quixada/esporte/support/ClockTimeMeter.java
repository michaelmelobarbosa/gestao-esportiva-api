package br.gov.quixada.esporte.support;

import io.github.bucket4j.TimeMeter;

import java.time.Clock;
import java.util.Objects;

/**
 * Adapta um {@link Clock} de teste ao {@link TimeMeter} do bucket4j, permitindo
 * simular a passagem do tempo sem {@code Thread.sleep}. Produção usa o relógio
 * monotônico ({@code TimeMeter.SYSTEM_NANOTIME}).
 */
public final class ClockTimeMeter implements TimeMeter {

    private final Clock clock;

    public ClockTimeMeter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public long currentTimeNanos() {
        var instant = clock.instant();
        return Math.addExact(Math.multiplyExact(instant.getEpochSecond(), 1_000_000_000L), instant.getNano());
    }

    @Override
    public boolean isWallClockBased() {
        return true;
    }
}
