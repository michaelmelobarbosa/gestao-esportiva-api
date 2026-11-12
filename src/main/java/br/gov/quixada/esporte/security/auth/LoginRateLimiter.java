package br.gov.quixada.esporte.security.auth;

import br.gov.quixada.esporte.security.config.LoginRateLimitProperties;
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
 * Rate limit em memória do login, com dois buckets por tentativa:
 * um por IP (anti-spraying e DoS de CPU) e um por (IP, e-mail) (anti-força-bruta
 * em uma conta). Não é lockout: não há estado persistente nem bloqueio de conta.
 */
@Service
public class LoginRateLimiter {

    private static final String PREFIXO_IP = "ip:";
    private static final String PREFIXO_PAR = "par:";

    private final LoginRateLimitProperties propriedades;
    private final TimeMeter timeMeter;
    private final Cache<String, Bucket> buckets;

    public LoginRateLimiter(LoginRateLimitProperties propriedades, TimeMeter timeMeter) {
        this.propriedades = Objects.requireNonNull(propriedades, "propriedades");
        this.timeMeter = Objects.requireNonNull(timeMeter, "timeMeter");

        var janela = propriedades.janela();
        if (janela == null || janela.isZero() || janela.isNegative()) {
            throw new IllegalArgumentException("api.security.login-rate-limit.janela deve ser positiva");
        }

        this.buckets = Caffeine.newBuilder()
                .maximumSize(propriedades.maximoChaves())
                .expireAfterAccess(janela.multipliedBy(2))
                .build();
    }

    /**
     * Consome uma tentativa dos dois buckets. Retorno vazio significa permitido;
     * presente significa bloqueado pelo tempo informado.
     */
    public Optional<Duration> tentar(String ip, String email) {
        var bucketIp = bucket(chaveIp(ip), propriedades.maxPorIp());
        var probeIp = bucketIp.tryConsumeAndReturnRemaining(1);
        if (!probeIp.isConsumed()) {
            return espera(probeIp);
        }

        var bucketPar = bucket(chavePar(ip, email), propriedades.maxPorIpEmail());
        var probePar = bucketPar.tryConsumeAndReturnRemaining(1);
        if (!probePar.isConsumed()) {
            bucketIp.addTokens(1);
            return espera(probePar);
        }

        return Optional.empty();
    }

    /** Devolve a tentativa consumida quando o login tem sucesso. */
    public void devolver(String ip, String email) {
        var bucketIp = buckets.getIfPresent(chaveIp(ip));
        if (bucketIp != null) {
            bucketIp.addTokens(1);
        }

        var bucketPar = buckets.getIfPresent(chavePar(ip, email));
        if (bucketPar != null) {
            bucketPar.addTokens(1);
        }
    }

    private Bucket bucket(String chave, long capacidade) {
        return buckets.get(chave, k -> Bucket.builder()
                .withCustomTimePrecision(timeMeter)
                .addLimit(limite -> limite.capacity(capacidade).refillGreedy(capacidade, propriedades.janela()))
                .build());
    }

    private static Optional<Duration> espera(ConsumptionProbe probe) {
        return Optional.of(Duration.ofNanos(probe.getNanosToWaitForRefill()));
    }

    private static String chaveIp(String ip) {
        return PREFIXO_IP + (ip == null ? "" : ip);
    }

    private static String chavePar(String ip, String email) {
        return PREFIXO_PAR + (ip == null ? "" : ip) + "|" + normalizar(email);
    }

    private static String normalizar(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
