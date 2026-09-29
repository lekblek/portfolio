package com.scalke.portfolio.backend.security.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Identifiants saisis à la connexion (D-CO). Bornes larges : elles limitent la taille de la requête, la vraie règle
 * est la comparaison au compte. {@link #toString()} masque le mot de passe.
 */
public record OpenAdminSessionRequest(
    @NotBlank @Size(max = 100) String login,
    @NotBlank @Size(max = 200) String password
) {

    @Override
    public String toString() {
        return "OpenAdminSessionRequest[login=" + login + ", password=******]";
    }
}
