package br.gov.quixada.sports.support;

import io.github.bucket4j.TimeMeter;

import java.time.Clock;
import java.util.Objects;

/**
 * Adapts a test {@link Clock} to the bucket4j {@link TimeMeter}, allowing time
 * to pass in tests without {@code Thread.sleep}. Production uses the monotonic
 * clock ({@code TimeMeter.SYSTEM_NANOTIME}).
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
