package br.gov.quixada.esporte.security.config;

import io.github.bucket4j.TimeMeter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    /**
     * Relógio monotônico: um ajuste de NTP não recarrega os buckets nem produz
     * {@code Retry-After} absurdo. Testes que precisam controlar a janela constroem
     * o {@link io.github.bucket4j.TimeMeter} diretamente.
     */
    @Bean
    public TimeMeter rateLimitTimeMeter() {
        return TimeMeter.SYSTEM_NANOTIME;
    }
}
