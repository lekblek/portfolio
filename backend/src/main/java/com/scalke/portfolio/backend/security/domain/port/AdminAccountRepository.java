package com.scalke.portfolio.backend.security.domain.port;

import com.scalke.portfolio.backend.security.domain.model.AdminAccount;

import java.util.Optional;

/**
 * Port de persistance du compte administrateur unique. Méthodes utilisées par
 * {@code InitializeAdminAccountUseCase} ; la lecture pour la connexion et la date de dernière connexion arriveront
 * avec l'étape 34.
 */
public interface AdminAccountRepository {

    Optional<AdminAccount> find();

    AdminAccount create(AdminAccount account);

    /**
     * Enregistre un nouvel identifiant ou une nouvelle empreinte : seuls {@code login}, {@code passwordHash} et
     * {@code updatedAt} sont écrits.
     */
    AdminAccount updateCredentials(AdminAccount account);
}
