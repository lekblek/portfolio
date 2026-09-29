package com.scalke.portfolio.backend.security.infrastructure;

import org.springframework.boot.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Réglage du serveur HTTP (D-CR, KI-33). Par défaut, Tomcat rejette un séparateur encodé ({@code %2F}) avant
 * l'application, avec une page d'erreur HTML. Transmis tel quel, il est rejeté juste après par le pare-feu HTTP de
 * Spring Security ({@code StrictHttpFirewall}), avec une erreur de même forme que les autres (400
 * {@code MALFORMED_REQUEST}, D-CL) : le rejet a toujours lieu avant toute route.
 */
@Configuration
public class HttpServerConfiguration {

    @Bean
    TomcatConnectorCustomizer encodedSolidusToFirewall() {
        return connector -> connector.setEncodedSolidusHandling("passthrough");
    }
}
