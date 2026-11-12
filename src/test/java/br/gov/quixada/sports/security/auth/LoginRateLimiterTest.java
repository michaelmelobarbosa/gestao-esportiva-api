package br.gov.quixada.sports.security.auth;

import br.gov.quixada.sports.security.config.LoginRateLimitProperties;
import br.gov.quixada.sports.support.ClockTimeMeter;
import br.gov.quixada.sports.support.ControllerTestSupport;
import br.gov.quixada.sports.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("LoginRateLimiter")
class LoginRateLimiterTest {

    private static final String IP = "10.0.0.1";
    private static final String OTHER_IP = "10.0.0.2";
    private static final String EMAIL = "john@example.com";
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private MutableClock clock;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(ControllerTestSupport.FIXED_INSTANT, ControllerTestSupport.FIXED_ZONE);
    }

    @Test
    @DisplayName("should allow attempts up to the (IP, email) pair limit")
    void shouldAllowUpToPairLimit() {
        var limiter = limiter(100, 2);

        assertThat(limiter.attempt(IP, EMAIL)).isEmpty();
        assertThat(limiter.attempt(IP, EMAIL)).isEmpty();

        var blocked = limiter.attempt(IP, EMAIL);
        assertThat(blocked).isPresent();
        assertThat(blocked.get()).isPositive();
    }

    @Test
    @DisplayName("should not affect another IP")
    void shouldNotAffectOtherIp() {
        var limiter = limiter(100, 1);

        assertThat(limiter.attempt(IP, EMAIL)).isEmpty();
        assertThat(limiter.attempt(IP, EMAIL)).isPresent();

        assertThat(limiter.attempt(OTHER_IP, EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("should return the attempt when the login succeeds")
    void shouldReturnAttemptOnSuccess() {
        var limiter = limiter(100, 1);

        assertThat(limiter.attempt(IP, EMAIL)).isEmpty();
        assertThat(limiter.attempt(IP, EMAIL)).isPresent();

        limiter.returnAttempt(IP, EMAIL);

        assertThat(limiter.attempt(IP, EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("should respect the capacity when returning attempts")
    void shouldRespectCapacityWhenReturning() {
        var limiter = limiter(100, 1);

        assertThat(limiter.attempt(IP, EMAIL)).isEmpty();
        limiter.returnAttempt(IP, EMAIL);
        limiter.returnAttempt(IP, EMAIL);

        assertThat(limiter.attempt(IP, EMAIL)).isEmpty();
        assertThat(limiter.attempt(IP, EMAIL)).isPresent();
    }

    @Test
    @DisplayName("should free again when the window expires")
    void shouldFreeAgainWhenWindowExpires() {
        var limiter = limiter(100, 1);

        assertThat(limiter.attempt(IP, EMAIL)).isEmpty();
        assertThat(limiter.attempt(IP, EMAIL)).isPresent();

        clock.advance(WINDOW);

        assertThat(limiter.attempt(IP, EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("should limit the IP even with distinct emails")
    void shouldLimitIpAcrossDistinctEmails() {
        var limiter = limiter(2, 100);

        assertThat(limiter.attempt(IP, "a@example.com")).isEmpty();
        assertThat(limiter.attempt(IP, "b@example.com")).isEmpty();
        assertThat(limiter.attempt(IP, "c@example.com")).isPresent();
    }

    @Test
    @DisplayName("should normalize the email in the pair key")
    void shouldNormalizeEmailInKey() {
        var limiter = limiter(100, 1);

        assertThat(limiter.attempt(IP, "  John@Example.com ")).isEmpty();
        assertThat(limiter.attempt(IP, "john@example.com")).isPresent();
    }

    @Test
    @DisplayName("should report a wait proportional to the refill")
    void shouldReportWaitProportionalToRefill() {
        var limiter = limiter(100, 5);

        for (int i = 0; i < 5; i++) {
            assertThat(limiter.attempt(IP, EMAIL)).isEmpty();
        }

        var wait = limiter.attempt(IP, EMAIL);
        assertThat(wait).isPresent();
        assertThat(wait.get()).isBetween(Duration.ofSeconds(179), Duration.ofSeconds(181));
    }

    @Test
    @DisplayName("should compensate the IP token when the pair blocks")
    void shouldCompensateIpTokenWhenPairBlocks() {
        var limiter = limiter(2, 1);

        assertThat(limiter.attempt(IP, EMAIL)).isEmpty();
        assertThat(limiter.attempt(IP, EMAIL)).isPresent();

        // without the compensation, the IP bucket would already be empty and would block this new email
        assertThat(limiter.attempt(IP, "outro@example.com")).isEmpty();
    }

    @Test
    @DisplayName("should not fail when returning without a consumed attempt")
    void shouldNotFailReturningWithoutConsumedAttempt() {
        var limiter = limiter(100, 1);

        assertThatCode(() -> limiter.returnAttempt(IP, EMAIL)).doesNotThrowAnyException();
        assertThat(limiter.attempt(IP, EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("should respect the limit under concurrency")
    void shouldRespectLimitUnderConcurrency() {
        var limiter = limiter(1000, 10);
        var allowed = new AtomicInteger();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 50; i++) {
                executor.submit(() -> {
                    if (limiter.attempt(IP, EMAIL).isEmpty()) {
                        allowed.incrementAndGet();
                    }
                });
            }
        }

        assertThat(allowed.get()).isEqualTo(10);
    }

    private LoginRateLimiter limiter(long maxPerIp, long maxPerIpEmail) {
        var properties = new LoginRateLimitProperties(maxPerIp, maxPerIpEmail, WINDOW, 1000);
        return new LoginRateLimiter(properties, new ClockTimeMeter(clock));
    }
}
