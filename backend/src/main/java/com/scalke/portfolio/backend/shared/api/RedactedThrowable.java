package com.scalke.portfolio.backend.shared.api;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Copie d'une exception pour les journaux (D-DB) : mêmes classes, mêmes piles d'appels, mêmes causes, mais **sans les
 * messages**, qui peuvent contenir un mot de passe (URL JDBC), une adresse ou le texte d'un message de contact, une
 * valeur saisie reprise par PostgreSQL. Le diagnostic garde le type de chaque exception, l'endroit où elle a été
 * levée et, pour une erreur SQL, son code d'état (non sensible).
 */
final class RedactedThrowable extends RuntimeException {

    private RedactedThrowable(String description) {
        // Pas de cause au constructeur : elle est posée ensuite par initCause, avec la copie de la cause d'origine.
        super(description);
    }

    static RedactedThrowable of(Throwable original) {
        return copy(original, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static RedactedThrowable copy(Throwable original, Set<Throwable> seen) {
        seen.add(original);
        RedactedThrowable copy = new RedactedThrowable(describe(original));
        copy.setStackTrace(original.getStackTrace());
        Throwable cause = original.getCause();
        if (cause != null && !seen.contains(cause)) {
            copy.initCause(copy(cause, seen));
        }
        for (Throwable suppressed : original.getSuppressed()) {
            if (!seen.contains(suppressed)) {
                copy.addSuppressed(copy(suppressed, seen));
            }
        }
        return copy;
    }

    private static String describe(Throwable original) {
        String type = original.getClass().getName();
        if (original instanceof SQLException sql && sql.getSQLState() != null) {
            return type + " [SQLState " + sql.getSQLState() + "] (message masqué)";
        }
        return type + " (message masqué)";
    }
}
