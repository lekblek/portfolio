package com.scalke.portfolio.backend.shared.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D-DH : le nom vient du champ {@code n} de l'erreur PostgreSQL, jamais du texte, qui peut être traduit ou contenir le
 * nom d'une autre contrainte parmi les valeurs recopiées.
 */
class ViolatedConstraintTest {

    @Test
    void reads_the_constraint_field_of_the_postgresql_error() {
        ServerErrorMessage error = new ServerErrorMessage("SERREUR\0C23505\0"
            + "Mla valeur d'une clé dupliquée rompt la contrainte unique « category_name_unique_idx »\0"
            + "DLa clé « (lower(name))=(x_slug_unique) » existe déjà.\0ncategory_name_unique_idx\0");

        assertThat(ViolatedConstraint.of(wrapped(new PSQLException(error)))).contains("category_name_unique_idx");
    }

    @Test
    void is_empty_without_a_named_postgresql_constraint() {
        ServerErrorMessage unnamed = new ServerErrorMessage("SERROR\0C23502\0Mnull value in column \"name\"\0");

        assertThat(ViolatedConstraint.of(wrapped(new PSQLException(unnamed)))).isEmpty();
        assertThat(ViolatedConstraint.of(wrapped(new PSQLException("connexion perdue", PSQLState.CONNECTION_FAILURE))))
            .isEmpty();
        assertThat(ViolatedConstraint.of(new DataIntegrityViolationException("sans cause"))).isEmpty();
    }

    /**
     * Chaîne réelle : exception de Spring, puis de Hibernate, puis du pilote.
     */
    private static DataIntegrityViolationException wrapped(PSQLException driver) {
        return new DataIntegrityViolationException("refus", new IllegalStateException("hibernate", driver));
    }
}
