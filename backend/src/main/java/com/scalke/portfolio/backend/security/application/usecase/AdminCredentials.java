package com.scalke.portfolio.backend.security.application.usecase;

import com.scalke.portfolio.backend.security.domain.model.AdminAccount;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Identifiant et mot de passe de l'administrateur, lus dans la configuration (D18, D-CN).
 * <p>
 * Mot de passe : 15 caractères au moins (seul facteur d'authentification : recommandation du NIST SP 800-63B),
 * 72 octets UTF-8 au plus (au-delà, bcrypt refuse ; l'ancien comportement était de tronquer). Aucun message ni
 * {@link #toString()} ne contient le mot de passe.
 */
public record AdminCredentials(String login, String password) {

    public static final int PASSWORD_MIN_LENGTH = 15;
    public static final int PASSWORD_MAX_BYTES = 72;

    public AdminCredentials {
        Objects.requireNonNull(login, "login");
        Objects.requireNonNull(password, "password");
        if (!AdminAccount.LOGIN.matcher(login).matches()) {
            throw new IllegalArgumentException(
                "ADMIN_USERNAME : 3 à 100 caractères parmi lettres, chiffres et . _ @ + -");
        }
        if (password.codePointCount(0, password.length()) < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException(
                "ADMIN_PASSWORD : " + PASSWORD_MIN_LENGTH + " caractères au moins");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
            throw new IllegalArgumentException(
                "ADMIN_PASSWORD : " + PASSWORD_MAX_BYTES + " octets au plus (limite de bcrypt)");
        }
    }

    @Override
    public String toString() {
        return "AdminCredentials[login=" + login + ", password=******]";
    }
}
