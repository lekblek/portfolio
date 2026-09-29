package com.scalke.portfolio.backend.security.domain.port;

import com.scalke.portfolio.backend.security.domain.model.AdminAccount;

import java.util.Optional;

/**
 * Port de persistance du compte administrateur unique. Méthodes utilisées par {@code InitializeAdminAccountUseCase}
 * ({@code find}, {@code create}, {@code updateCredentials}), par la connexion ({@code find} pour
 * {@code AdminUserDetailsService}, {@code recordLogin} pour {@code AuthenticateAdminUseCase}) et par
 * {@code GetAdminAccountUseCase} ({@code find}).
 */
public interface AdminAccountRepository {

    Optional<AdminAccount> find();

    AdminAccount create(AdminAccount account);

    /**
     * Enregistre un nouvel identifiant ou une nouvelle empreinte : seuls {@code login}, {@code passwordHash} et
     * {@code updatedAt} sont écrits.
     */
    AdminAccount updateCredentials(AdminAccount account);

    /**
     * Enregistre la date de connexion : seul {@code lastLoginAt} est écrit ; {@code updatedAt} ne change pas (une
     * connexion ne modifie pas le compte).
     */
    AdminAccount recordLogin(AdminAccount account);
}
