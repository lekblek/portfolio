package com.scalke.portfolio.backend.security;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contraintes de {@code V021__create_admin_account.sql} (D-CM), vérifiées sans JPA. Une seule violation par test.
 */
@Transactional
class AdminAccountSchemaIT extends AbstractIntegrationTest {

    private static final String HASH = "{bcrypt}$2a$10$zPzd.q.vE0pkkBWPr9yFtemfAy/x6WkCtf4NRVNwC/fWmS0RxZ90u";
    private static final OffsetDateTime AT = OffsetDateTime.parse("2026-06-15T10:00:00Z");

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void accepts_one_account_with_a_bcrypt_hash() {
        insert(1, "admin", HASH);

        assertThat(jdbcClient.sql("SELECT enabled FROM admin_account").query(Boolean.class).single()).isTrue();
    }

    /**
     * Invariant 30 : il existe au plus un compte administrateur.
     */
    @Test
    void refuses_a_second_account() {
        assertThatThrownBy(() -> insert(2, "autre", HASH))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("admin_account_single_row");
    }

    /**
     * Invariant 15 : un mot de passe en clair, ou préfixé par {@code {noop}}, n'est jamais stocké.
     */
    @ParameterizedTest
    @ValueSource(strings = {"un-mot-de-passe-en-clair", "{noop}un-mot-de-passe-en-clair",
        "$2a$10$zPzd.q.vE0pkkBWPr9yFtemfAy/x6WkCtf4NRVNwC/fWmS0RxZ90u"})
    void refuses_anything_but_a_bcrypt_hash(String value) {
        assertThatThrownBy(() -> insert(1, "admin", value))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("admin_account_password_hash_check");
    }

    @Test
    void refuses_an_invalid_login() {
        assertThatThrownBy(() -> insert(1, "admin root", HASH))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("admin_account_login_check");
    }

    private void insert(long id, String login, String passwordHash) {
        jdbcClient.sql("""
                    INSERT INTO admin_account (id, login, password_hash, created_at, updated_at)
                    VALUES (:id, :login, :passwordHash, :at, :at)
                    """)
            .param("id", id)
            .param("login", login)
            .param("passwordHash", passwordHash)
            .param("at", AT)
            .update();
    }
}
