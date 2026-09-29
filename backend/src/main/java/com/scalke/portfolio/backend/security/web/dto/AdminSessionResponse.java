package com.scalke.portfolio.backend.security.web.dto;

import com.scalke.portfolio.backend.security.domain.model.AdminAccount;

import java.time.Instant;

/**
 * Administrateur connecté (D-CO) : identifiant et date de la connexion en cours. Jamais l'empreinte.
 */
public record AdminSessionResponse(String login, Instant lastLoginAt) {

    public static AdminSessionResponse from(AdminAccount account) {
        return new AdminSessionResponse(account.login(), account.lastLoginAt());
    }
}
