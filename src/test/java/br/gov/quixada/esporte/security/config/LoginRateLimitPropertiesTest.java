package br.gov.quixada.esporte.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginRateLimitProperties")
class LoginRateLimitPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfig.class);

    @Test
    @DisplayName("deve vincular valores válidos")
    void deveVincularValoresValidos() {
        runner.withPropertyValues(
                        "api.security.login-rate-limit.max-por-ip=10",
                        "api.security.login-rate-limit.max-por-ip-email=3",
                        "api.security.login-rate-limit.janela=15m",
                        "api.security.login-rate-limit.maximo-chaves=1000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var propriedades = context.getBean(LoginRateLimitProperties.class);
                    assertThat(propriedades.maxPorIp()).isEqualTo(10);
                    assertThat(propriedades.maxPorIpEmail()).isEqualTo(3);
                    assertThat(propriedades.janela()).isEqualTo(Duration.ofMinutes(15));
                    assertThat(propriedades.maximoChaves()).isEqualTo(1000);
                });
    }

    @Test
    @DisplayName("deve falhar com limite menor que 1")
    void deveFalharComLimiteInvalido() {
        runner.withPropertyValues(
                        "api.security.login-rate-limit.max-por-ip=0",
                        "api.security.login-rate-limit.max-por-ip-email=3",
                        "api.security.login-rate-limit.janela=15m",
                        "api.security.login-rate-limit.maximo-chaves=1000")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("deve falhar sem a janela")
    void deveFalharSemJanela() {
        runner.withPropertyValues(
                        "api.security.login-rate-limit.max-por-ip=10",
                        "api.security.login-rate-limit.max-por-ip-email=3",
                        "api.security.login-rate-limit.maximo-chaves=1000")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("deve falhar com janela não positiva")
    void deveFalharComJanelaNaoPositiva() {
        runner.withPropertyValues(
                        "api.security.login-rate-limit.max-por-ip=10",
                        "api.security.login-rate-limit.max-por-ip-email=3",
                        "api.security.login-rate-limit.janela=0s",
                        "api.security.login-rate-limit.maximo-chaves=1000")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("deve falhar com máximo de chaves menor que 1")
    void deveFalharComMaximoDeChavesInvalido() {
        runner.withPropertyValues(
                        "api.security.login-rate-limit.max-por-ip=10",
                        "api.security.login-rate-limit.max-por-ip-email=3",
                        "api.security.login-rate-limit.janela=15m",
                        "api.security.login-rate-limit.maximo-chaves=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(LoginRateLimitProperties.class)
    static class PropertiesConfig {
    }
}
