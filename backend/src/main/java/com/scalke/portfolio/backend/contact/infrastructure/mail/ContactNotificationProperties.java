package com.scalke.portfolio.backend.contact.infrastructure.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * Adresses des notifications de contact ({@code portfolio.contact.notification.*}, D-CI) : {@code from},
 * l'expéditeur accepté par le serveur SMTP ; {@code to}, l'administrateur. Vide refusé au démarrage.
 */
@ConfigurationProperties("portfolio.contact.notification")
public record ContactNotificationProperties(String from, String to) {

    public ContactNotificationProperties {
        requireAddress(from, "portfolio.contact.notification.from");
        requireAddress(to, "portfolio.contact.notification.to");
    }

    private static void requireAddress(String value, String property) {
        Objects.requireNonNull(value, property);
        if (value.isBlank()) {
            throw new IllegalArgumentException(property + " must not be empty");
        }
    }
}
