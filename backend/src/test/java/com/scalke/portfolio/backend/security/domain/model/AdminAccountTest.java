package com.scalke.portfolio.backend.security.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminAccountTest {

    static final String HASH = "{bcrypt}$2a$10$zPzd.q.vE0pkkBWPr9yFtemfAy/x6WkCtf4NRVNwC/fWmS0RxZ90u";
    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");

    @Test
    void a_created_account_is_enabled_and_has_never_logged_in() {
        AdminAccount account = AdminAccount.create("admin", HASH, NOW);

        assertThat(account.enabled()).isTrue();
        assertThat(account.lastLoginAt()).isNull();
        assertThat(account.createdAt()).isEqualTo(NOW);
        assertThat(account.updatedAt()).isEqualTo(NOW);
    }

    /**
     * Invariant 15 : seule une empreinte bcrypt est acceptée ; le refus ne répète jamais la valeur reçue.
     */
    @ParameterizedTest
    @ValueSource(strings = {"un-mot-de-passe-en-clair", "{noop}un-mot-de-passe-en-clair", "$2a$10$sansPrefixe",
        "{bcrypt}$2a$10$tropCourt"})
    void refuses_anything_but_a_bcrypt_hash_without_repeating_it(String value) {
        assertThatThrownBy(() -> AdminAccount.create("admin", value, NOW))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageNotContaining(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ad", "admin root", "admin\n", "ädmin"})
    void refuses_an_invalid_login(String login) {
        assertThatThrownBy(() -> AdminAccount.create(login, HASH, NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accepts_an_email_address_as_login() {
        assertThat(AdminAccount.create("blek+admin@example.com", HASH, NOW).login()).isEqualTo("blek+admin@example.com");
    }

    @Test
    void never_prints_its_password_hash() {
        assertThat(AdminAccount.create("admin", HASH, NOW).toString()).doesNotContain(HASH).contains("admin");
    }
}
