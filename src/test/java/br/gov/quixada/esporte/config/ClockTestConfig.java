package br.gov.quixada.esporte.config;

import java.time.Clock;

import br.gov.quixada.esporte.support.ControllerTestSupport;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Substitui o {@link Clock} do sistema por um instante fixo nos testes que
 * sobem o contexto Spring. Importe com {@code @Import(ClockTestConfig.class)}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ClockTestConfig {

    @Bean
    @Primary
    public Clock fixedClock() {
        return ControllerTestSupport.FIXED_CLOCK;
    }
}
