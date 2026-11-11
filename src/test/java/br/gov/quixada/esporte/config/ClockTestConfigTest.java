package br.gov.quixada.esporte.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import br.gov.quixada.esporte.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("ClockTestConfig")
class ClockTestConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ClockConfig.class, ClockTestConfig.class);

    @Test
    @DisplayName("deve priorizar o Clock fixo sobre o Clock do sistema")
    void devePriorizarClockFixo() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(Clock.class)).isSameAs(ControllerTestSupport.FIXED_CLOCK);
        });
    }
}
