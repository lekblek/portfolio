package com.scalke.portfolio.backend.security.application.usecase;

import com.scalke.portfolio.backend.security.domain.model.AdminAccount;
import org.springframework.security.core.Authentication;

/**
 * Résultat d'une connexion réussie : l'authentification à placer dans la session et le compte connecté.
 */
public record AuthenticatedAdmin(Authentication authentication, AdminAccount account) {
}
