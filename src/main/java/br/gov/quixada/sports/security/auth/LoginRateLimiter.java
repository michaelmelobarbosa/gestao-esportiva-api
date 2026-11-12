package br.gov.quixada.sports.security.auth;

import br.gov.quixada.sports.security.config.LoginRateLimitProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory login rate limiter with two buckets per attempt:
 * one per IP (anti-spraying and CPU DoS) and one per (IP, email) (anti-brute-force
 * on an account). It is not a lockout: there is no persistent state or account lock.
 */
@Service
public class LoginRateLimiter {

    private static final String IP_PREFIX = "ip:";
    private static final String PAIR_PREFIX = "par:";

    private final LoginRateLimitProperties properties;
    private final TimeMeter timeMeter;
    private final Cache<String, Bucket> buckets;

    public LoginRateLimiter(LoginRateLimitProperties properties, TimeMeter timeMeter) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.timeMeter = Objects.requireNonNull(timeMeter, "timeMeter");

        var window = properties.window();
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("api.security.login-rate-limit.window must be positive");
        }

        this.buckets = Caffeine.newBuilder()
                .maximumSize(properties.maxKeys())
                .expireAfterAccess(window.multipliedBy(2))
                .build();
    }

    /**
     * Consumes one attempt from both buckets. Empty result means allowed;
     * present means blocked for the given time.
     */
    public Optional<Duration> attempt(String ip, String email) {
        var bucketIp = bucket(ipKey(ip), properties.maxPerIp());
        var probeIp = bucketIp.tryConsumeAndReturnRemaining(1);
        if (!probeIp.isConsumed()) {
            return waitTime(probeIp);
        }

        var bucketPar = bucket(pairKey(ip, email), properties.maxPerIpEmail());
        var probePar = bucketPar.tryConsumeAndReturnRemaining(1);
        if (!probePar.isConsumed()) {
            bucketIp.addTokens(1);
            return waitTime(probePar);
        }

        return Optional.empty();
    }

    /** Returns the consumed attempt when login succeeds. */
    public void returnAttempt(String ip, String email) {
        var bucketIp = buckets.getIfPresent(ipKey(ip));
        if (bucketIp != null) {
            bucketIp.addTokens(1);
        }

        var bucketPar = buckets.getIfPresent(pairKey(ip, email));
        if (bucketPar != null) {
            bucketPar.addTokens(1);
        }
    }

    private Bucket bucket(String key, long capacity) {
        return buckets.get(key, k -> Bucket.builder()
                .withCustomTimePrecision(timeMeter)
                .addLimit(limite -> limite.capacity(capacity).refillGreedy(capacity, properties.window()))
                .build());
    }

    private static Optional<Duration> waitTime(ConsumptionProbe probe) {
        return Optional.of(Duration.ofNanos(probe.getNanosToWaitForRefill()));
    }

    private static String ipKey(String ip) {
        return IP_PREFIX + (ip == null ? "" : ip);
    }

    private static String pairKey(String ip, String email) {
        return PAIR_PREFIX + (ip == null ? "" : ip) + "|" + normalize(email);
    }

    private static String normalize(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
