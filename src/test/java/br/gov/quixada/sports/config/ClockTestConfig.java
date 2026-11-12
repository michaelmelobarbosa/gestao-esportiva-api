package br.gov.quixada.sports.config;

import java.time.Clock;

import br.gov.quixada.sports.support.ControllerTestSupport;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the system {@link Clock} with a fixed instant in tests that boot the
 * Spring context. Import with {@code @Import(ClockTestConfig.class)}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ClockTestConfig {

    @Bean
    @Primary
    public Clock fixedClock() {
        return ControllerTestSupport.FIXED_CLOCK;
    }
}
