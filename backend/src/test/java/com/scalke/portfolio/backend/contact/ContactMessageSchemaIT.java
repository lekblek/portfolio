package com.scalke.portfolio.backend.contact;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contraintes de {@code V020__create_contact_message.sql} (D-CG), vérifiées sans JPA.
 */
@Transactional
class ContactMessageSchemaIT extends AbstractIntegrationTest {

    private static final OffsetDateTime AT = OffsetDateTime.parse("2026-06-15T10:00:00Z");

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void accepts_a_well_formed_message() {
        insert("Camille", "camille@example.com", "Mission", "Bonjour.", "NEW", AT);

        assertThat(jdbcClient.sql("SELECT count(*) FROM contact_message").query(Long.class).single()).isEqualTo(1);
    }

    /**
     * Une seule violation par test : dans une transaction PostgreSQL, une instruction refusée rend les suivantes
     * impossibles.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "' \\t' | Sujet | Message | contact_message_name_check",
        "Nom    | '  '  | Message | contact_message_subject_check",
        "Nom    | Sujet | '\\n'   | contact_message_message_check"
    })
    void rejects_a_blank_name_subject_or_message(String name, String subject, String message, String constraint) {
        assertThatThrownBy(() -> insert(name.translateEscapes(), "a@example.com", subject, message.translateEscapes(),
            "NEW", AT))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining(constraint);
    }

    @ParameterizedTest
    @ValueSource(strings = {"camille", "camille@example", "cam ille@example.com", "camille@@example.com"})
    void rejects_an_address_that_is_not_one(String email) {
        assertThatThrownBy(() -> insert("Nom", email, "Sujet", "Message", "NEW", AT))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("contact_message_email_check");
    }

    @Test
    void rejects_an_unknown_status() {
        assertThatThrownBy(() -> insert("Nom", "a@example.com", "Sujet", "Message", "SPAM", AT))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("contact_message_status_check");
    }

    @Test
    void rejects_an_update_before_the_reception() {
        assertThatThrownBy(() -> insert("Nom", "a@example.com", "Sujet", "Message", "READ", AT.minusSeconds(1)))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("contact_message_dates_check");
    }

    @Test
    void bounds_the_message_to_5000_characters() {
        insert("Nom", "a@example.com", "Sujet", "m".repeat(5000), "NEW", AT);

        assertThatThrownBy(() -> insert("Nom", "a@example.com", "Sujet", "m".repeat(5001), "NEW", AT))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insert(String name, String email, String subject, String message, String status,
                        OffsetDateTime updatedAt) {
        jdbcClient.sql("""
                    INSERT INTO contact_message (name, email, subject, message, status, created_at, updated_at)
                    VALUES (:name, :email, :subject, :message, :status, :createdAt, :updatedAt)
                    """)
            .param("name", name)
            .param("email", email)
            .param("subject", subject)
            .param("message", message)
            .param("status", status)
            .param("createdAt", AT)
            .param("updatedAt", updatedAt)
            .update();
    }
}
