package com.scalke.portfolio.backend.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Horloge fixe des tests d'intégration (D-AG) : « maintenant » vaut toujours {@link #NOW}, ce qui rend
 * déterministes les règles temporelles (publication planifiée). Prioritaire sur l'horloge système.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockConfiguration {

    public static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
