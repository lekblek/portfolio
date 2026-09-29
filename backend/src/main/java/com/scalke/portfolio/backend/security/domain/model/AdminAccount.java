package com.scalke.portfolio.backend.security.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Compte administrateur unique ({@code 02} §26, D-CM). Il n'en existe qu'un (invariant 30) ; son mot de passe n'est
 * connu que par son empreinte (invariant 15). Règles doublées par PostgreSQL ({@code V021}).
 */
public record AdminAccount(
    String login,
    String passwordHash,
    boolean enabled,
    Instant lastLoginAt,
    Instant createdAt,
    Instant updatedAt
) {

    /**
     * Identifiant : lettres, chiffres et {@code . _ @ + -}, de 3 à 100 caractères (une adresse électronique convient).
     */
    public static final Pattern LOGIN = Pattern.compile("^[A-Za-z0-9._@+-]{3,100}$");

    /**
     * Empreinte bcrypt préfixée par l'identifiant de l'encodeur ({@code DelegatingPasswordEncoder}) : même expression
     * que {@code admin_account_password_hash_check}.
     */
    public static final Pattern PASSWORD_HASH = Pattern.compile("^\\{bcrypt}\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}$");

    public AdminAccount {
        Objects.requireNonNull(login, "login");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (!LOGIN.matcher(login).matches()) {
            throw new IllegalArgumentException("login is not a valid administrator login");
        }
        if (!PASSWORD_HASH.matcher(passwordHash).matches()) {
            // Jamais la valeur dans le message : ce pourrait être un mot de passe en clair.
            throw new IllegalArgumentException("passwordHash is not a bcrypt hash");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }

    /**
     * Compte créé à {@code now}, actif, jamais connecté.
     */
    public static AdminAccount create(String login, String passwordHash, Instant now) {
        return new AdminAccount(login, passwordHash, true, null, now, now);
    }

    /**
     * Même compte avec un nouvel identifiant ou une nouvelle empreinte ; {@code updatedAt} devient {@code now}.
     */
    public AdminAccount withCredentials(String newLogin, String newPasswordHash, Instant now) {
        return new AdminAccount(newLogin, newPasswordHash, enabled, lastLoginAt, createdAt, now);
    }

    /**
     * Ne révèle jamais l'empreinte (journaux, messages d'erreur).
     */
    @Override
    public String toString() {
        return "AdminAccount[login=" + login + ", enabled=" + enabled + "]";
    }
}
