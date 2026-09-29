package com.scalke.portfolio.backend.security.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Compte administrateur de la configuration ({@code portfolio.admin.*}, variables {@code ADMIN_USERNAME} et
 * {@code ADMIN_PASSWORD}, D18). Les deux vides : rien n'est configuré. {@link #toString()} masque le mot de passe.
 */
@ConfigurationProperties("portfolio.admin")
public record AdminAccountProperties(String username, String password) {

    boolean isConfigured() {
        return hasText(username) || hasText(password);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public String toString() {
        return "AdminAccountProperties[username=" + username + ", password=******]";
    }
}
