package com.scalke.portfolio.backend.shared.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Horloge applicative (D03, D-AG). Toute règle métier dépendant de l'heure (publication planifiée,
 * dates d'audit) lit « maintenant » dans ce {@link Clock}, jamais par {@code Instant.now()} : les tests
 * d'intégration le remplacent par une horloge fixe.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
