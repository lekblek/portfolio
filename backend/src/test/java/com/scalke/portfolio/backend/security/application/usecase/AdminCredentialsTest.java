package com.scalke.portfolio.backend.security.application.usecase;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminCredentialsTest {

    /**
     * Bornes incluses : 15 caractères (un emoji compte pour un caractère), 72 octets UTF-8 (un « é » en compte deux).
     */
    @Test
    void bounds_the_password_in_characters_and_in_bytes() {
        assertThat(new AdminCredentials("admin", "a".repeat(15)).password()).hasSize(15);
        assertThat(new AdminCredentials("admin", "é".repeat(36)).password()).hasSize(36);

        assertThatThrownBy(() -> new AdminCredentials("admin", "a".repeat(14)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("15 caractères");
        assertThatThrownBy(() -> new AdminCredentials("admin", "🔒".repeat(14)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("15 caractères");
        assertThatThrownBy(() -> new AdminCredentials("admin", "é".repeat(36) + "a"))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("72 octets");
    }

    @Test
    void refuses_an_invalid_login() {
        assertThatThrownBy(() -> new AdminCredentials("ad", "a".repeat(20)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ADMIN_USERNAME");
    }

    @Test
    void never_prints_the_password() {
        String password = "correct-horse-battery-staple";

        assertThat(new AdminCredentials("admin", password).toString()).doesNotContain(password).contains("admin");
        assertThatThrownBy(() -> new AdminCredentials("admin", "court"))
            .hasMessageNotContaining("court");
    }
}
