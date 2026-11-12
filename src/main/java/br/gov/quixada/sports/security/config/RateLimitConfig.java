package br.gov.quixada.sports.security.config;

import io.github.bucket4j.TimeMeter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    /**
     * Monotonic clock: an NTP adjustment does not refill the buckets or produce an
     * absurd {@code Retry-After}. Tests that need to control the window build
     * the {@link io.github.bucket4j.TimeMeter} directly.
     */
    @Bean
    public TimeMeter rateLimitTimeMeter() {
        return TimeMeter.SYSTEM_NANOTIME;
    }
}
