package br.gov.quixada.sports.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test {@link Clock} that can be advanced manually, allowing rate limit window
 * expiration to be simulated without {@code Thread.sleep}.
 */
public final class MutableClock extends Clock {

    private final AtomicReference<Instant> now;
    private final ZoneId zone;

    public MutableClock(Instant initial, ZoneId zone) {
        this(new AtomicReference<>(initial), zone);
    }

    private MutableClock(AtomicReference<Instant> now, ZoneId zone) {
        this.now = now;
        this.zone = zone;
    }

    public void advance(Duration duration) {
        now.updateAndGet(instant -> instant.plus(duration));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return new MutableClock(now, newZone);
    }

    @Override
    public Instant instant() {
        return now.get();
    }
}
