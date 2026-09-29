package com.scalke.portfolio.backend.shared.infrastructure.persistence;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

/**
 * Nom de la contrainte refusée par PostgreSQL (D-DH), lu dans le champ dédié de l'erreur du protocole
 * ({@code ServerErrorMessage.getConstraint()}) : indépendant de la langue du serveur et des valeurs saisies, que le
 * texte du message recopie. Pour un index unique, c'est le nom de l'index. Seule classe autorisée à dépendre du pilote
 * JDBC (ArchUnit).
 */
public final class ViolatedConstraint {

    private ViolatedConstraint() {
    }

    /**
     * @return le nom de la contrainte, ou vide si l'erreur ne vient pas de PostgreSQL ou n'en nomme aucune
     */
    public static Optional<String> of(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof PSQLException psql) {
                return Optional.ofNullable(psql.getServerErrorMessage()).map(ServerErrorMessage::getConstraint);
            }
        }
        return Optional.empty();
    }
}
