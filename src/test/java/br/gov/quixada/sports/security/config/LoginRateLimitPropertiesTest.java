package br.gov.quixada.sports.security.config;

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
    @DisplayName("should bind valid values")
    void shouldBindValidValues() {
        runner.withPropertyValues(
                        "api.security.login-rate-limit.max-per-ip=10",
                        "api.security.login-rate-limit.max-per-ip-email=3",
                        "api.security.login-rate-limit.window=15m",
                        "api.security.login-rate-limit.max-keys=1000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var properties = context.getBean(LoginRateLimitProperties.class);
                    assertThat(properties.maxPerIp()).isEqualTo(10);
                    assertThat(properties.maxPerIpEmail()).isEqualTo(3);
                    assertThat(properties.window()).isEqualTo(Duration.ofMinutes(15));
                    assertThat(properties.maxKeys()).isEqualTo(1000);
                });
    }

    @Test
    @DisplayName("should fail with a limit below 1")
    void shouldFailWithInvalidLimit() {
        runner.withPropertyValues(
                        "api.security.login-rate-limit.max-per-ip=0",
                        "api.security.login-rate-limit.max-per-ip-email=3",
                        "api.security.login-rate-limit.window=15m",
                        "api.security.login-rate-limit.max-keys=1000")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("should fail without the window")
    void shouldFailWithoutWindow() {
        runner.withPropertyValues(
                        "api.security.login-rate-limit.max-per-ip=10",
                        "api.security.login-rate-limit.max-per-ip-email=3",
                        "api.security.login-rate-limit.max-keys=1000")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("should fail with a non-positive window")
    void shouldFailWithNonPositiveWindow() {
        runner.withPropertyValues(
                        "api.security.login-rate-limit.max-per-ip=10",
                        "api.security.login-rate-limit.max-per-ip-email=3",
                        "api.security.login-rate-limit.window=0s",
                        "api.security.login-rate-limit.max-keys=1000")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("should fail with max keys below 1")
    void shouldFailWithInvalidMaxKeys() {
        runner.withPropertyValues(
                        "api.security.login-rate-limit.max-per-ip=10",
                        "api.security.login-rate-limit.max-per-ip-email=3",
                        "api.security.login-rate-limit.window=15m",
                        "api.security.login-rate-limit.max-keys=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(LoginRateLimitProperties.class)
    static class PropertiesConfig {
    }
}
