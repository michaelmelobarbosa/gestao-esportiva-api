package br.gov.quixada.sports.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import br.gov.quixada.sports.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("ClockTestConfig")
class ClockTestConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ClockConfig.class, ClockTestConfig.class);

    @Test
    @DisplayName("should prioritize the fixed Clock over the system Clock")
    void shouldPrioritizeFixedClock() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(Clock.class)).isSameAs(ControllerTestSupport.FIXED_CLOCK);
        });
    }
}
