package br.gov.quixada.esporte.security.auth;

import br.gov.quixada.esporte.security.config.LoginRateLimitProperties;
import br.gov.quixada.esporte.support.ClockTimeMeter;
import br.gov.quixada.esporte.support.ControllerTestSupport;
import br.gov.quixada.esporte.support.MutableClock;
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
    private static final String OUTRO_IP = "10.0.0.2";
    private static final String EMAIL = "fulano@example.com";
    private static final Duration JANELA = Duration.ofMinutes(15);

    private MutableClock clock;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(ControllerTestSupport.FIXED_INSTANT, ControllerTestSupport.FIXED_ZONE);
    }

    @Test
    @DisplayName("deve permitir tentativas até o limite do par (IP, e-mail)")
    void devePermitirAteOLimiteDoPar() {
        var limiter = limiter(100, 2);

        assertThat(limiter.tentar(IP, EMAIL)).isEmpty();
        assertThat(limiter.tentar(IP, EMAIL)).isEmpty();

        var bloqueio = limiter.tentar(IP, EMAIL);
        assertThat(bloqueio).isPresent();
        assertThat(bloqueio.get()).isPositive();
    }

    @Test
    @DisplayName("não deve afetar outro IP")
    void naoDeveAfetarOutroIp() {
        var limiter = limiter(100, 1);

        assertThat(limiter.tentar(IP, EMAIL)).isEmpty();
        assertThat(limiter.tentar(IP, EMAIL)).isPresent();

        assertThat(limiter.tentar(OUTRO_IP, EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("deve devolver a tentativa quando o login tem sucesso")
    void deveDevolverTentativaNoSucesso() {
        var limiter = limiter(100, 1);

        assertThat(limiter.tentar(IP, EMAIL)).isEmpty();
        assertThat(limiter.tentar(IP, EMAIL)).isPresent();

        limiter.devolver(IP, EMAIL);

        assertThat(limiter.tentar(IP, EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("deve respeitar a capacidade ao devolver tentativas")
    void deveRespeitarCapacidadeAoDevolver() {
        var limiter = limiter(100, 1);

        assertThat(limiter.tentar(IP, EMAIL)).isEmpty();
        limiter.devolver(IP, EMAIL);
        limiter.devolver(IP, EMAIL);

        assertThat(limiter.tentar(IP, EMAIL)).isEmpty();
        assertThat(limiter.tentar(IP, EMAIL)).isPresent();
    }

    @Test
    @DisplayName("deve liberar novamente quando a janela expira")
    void deveLiberarQuandoJanelaExpira() {
        var limiter = limiter(100, 1);

        assertThat(limiter.tentar(IP, EMAIL)).isEmpty();
        assertThat(limiter.tentar(IP, EMAIL)).isPresent();

        clock.advance(JANELA);

        assertThat(limiter.tentar(IP, EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("deve limitar o IP mesmo com e-mails distintos")
    void deveLimitarIpComEmailsDistintos() {
        var limiter = limiter(2, 100);

        assertThat(limiter.tentar(IP, "a@example.com")).isEmpty();
        assertThat(limiter.tentar(IP, "b@example.com")).isEmpty();
        assertThat(limiter.tentar(IP, "c@example.com")).isPresent();
    }

    @Test
    @DisplayName("deve normalizar o e-mail na chave do par")
    void deveNormalizarEmailNaChave() {
        var limiter = limiter(100, 1);

        assertThat(limiter.tentar(IP, "  Fulano@Example.com ")).isEmpty();
        assertThat(limiter.tentar(IP, "fulano@example.com")).isPresent();
    }

    @Test
    @DisplayName("deve informar espera proporcional ao refill")
    void deveInformarEsperaProporcionalAoRefill() {
        var limiter = limiter(100, 5);

        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tentar(IP, EMAIL)).isEmpty();
        }

        var espera = limiter.tentar(IP, EMAIL);
        assertThat(espera).isPresent();
        assertThat(espera.get()).isBetween(Duration.ofSeconds(179), Duration.ofSeconds(181));
    }

    @Test
    @DisplayName("deve compensar o token do IP quando o par bloqueia")
    void deveCompensarTokenDoIpQuandoParBloqueia() {
        var limiter = limiter(2, 1);

        assertThat(limiter.tentar(IP, EMAIL)).isEmpty();
        assertThat(limiter.tentar(IP, EMAIL)).isPresent();

        // sem a compensação, o bucket de IP já estaria zerado e bloquearia este e-mail novo
        assertThat(limiter.tentar(IP, "outro@example.com")).isEmpty();
    }

    @Test
    @DisplayName("não deve falhar ao devolver sem tentativa consumida")
    void naoDeveFalharAoDevolverSemTentativa() {
        var limiter = limiter(100, 1);

        assertThatCode(() -> limiter.devolver(IP, EMAIL)).doesNotThrowAnyException();
        assertThat(limiter.tentar(IP, EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("deve respeitar o limite sob concorrência")
    void deveRespeitarLimiteSobConcorrencia() {
        var limiter = limiter(1000, 10);
        var permitidos = new AtomicInteger();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 50; i++) {
                executor.submit(() -> {
                    if (limiter.tentar(IP, EMAIL).isEmpty()) {
                        permitidos.incrementAndGet();
                    }
                });
            }
        }

        assertThat(permitidos.get()).isEqualTo(10);
    }

    private LoginRateLimiter limiter(long maxPorIp, long maxPorIpEmail) {
        var propriedades = new LoginRateLimitProperties(maxPorIp, maxPorIpEmail, JANELA, 1000);
        return new LoginRateLimiter(propriedades, new ClockTimeMeter(clock));
    }
}
