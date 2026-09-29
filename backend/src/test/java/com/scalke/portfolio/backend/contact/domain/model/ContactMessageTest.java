package com.scalke.portfolio.backend.contact.domain.model;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContactMessageTest {

    private static final Instant RECEIVED = Instant.parse("2026-06-15T10:00:00Z");
    private static final Instant LATER = RECEIVED.plus(Duration.ofHours(1));

    @Test
    void a_submitted_message_is_new_and_stripped() {
        ContactMessage message = ContactMessage.submit("  Camille  ", " camille@example.com ", " Mission ",
            "\n  Bonjour.\n  ", RECEIVED);

        assertThat(message.id()).isNull();
        assertThat(message.name()).isEqualTo("Camille");
        assertThat(message.email()).isEqualTo("camille@example.com");
        assertThat(message.subject()).isEqualTo("Mission");
        assertThat(message.message()).isEqualTo("Bonjour.");
        assertThat(message.status()).isEqualTo(ContactStatus.NEW);
        assertThat(message.createdAt()).isEqualTo(RECEIVED);
        assertThat(message.updatedAt()).isEqualTo(RECEIVED);
    }

    @Test
    void rejects_a_blank_field() {
        assertThatThrownBy(() -> submit(" \t ", "a@example.com", "Sujet", "Message"))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
        assertThatThrownBy(() -> submit("Nom", "a@example.com", "", "Message"))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("subject");
        assertThatThrownBy(() -> submit("Nom", "a@example.com", "Sujet", "\n"))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("message");
        assertThatThrownBy(() -> submit(null, "a@example.com", "Sujet", "Message"))
            .isInstanceOf(NullPointerException.class);
    }

    /**
     * Bornes incluses ; même valeur que les colonnes de {@code V020}.
     */
    @Test
    void bounds_the_length_of_each_field() {
        String domain = "@example.com";
        ContactMessage longest = submit("n".repeat(ContactMessage.NAME_MAX_LENGTH),
            "e".repeat(ContactMessage.EMAIL_MAX_LENGTH - domain.length()) + domain,
            "s".repeat(ContactMessage.SUBJECT_MAX_LENGTH), "m".repeat(ContactMessage.MESSAGE_MAX_LENGTH));
        assertThat(longest.message()).hasSize(5000);

        assertThatThrownBy(() -> submit("n".repeat(101), "a@example.com", "Sujet", "Message"))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
        assertThatThrownBy(() -> submit("Nom", "e".repeat(243) + domain, "Sujet", "Message"))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("email");
        assertThatThrownBy(() -> submit("Nom", "a@example.com", "s".repeat(201), "Message"))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("subject");
        assertThatThrownBy(() -> submit("Nom", "a@example.com", "Sujet", "m".repeat(5001)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("message");
    }

    @ParameterizedTest
    @ValueSource(strings = {"camille", "camille@", "@example.com", "camille@example", "cam ille@example.com",
        "camille@@example.com"})
    void rejects_an_address_that_is_not_one(String email) {
        assertThatThrownBy(() -> submit("Nom", email, "Sujet", "Message"))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("email");
    }

    @Test
    void moving_forward_changes_the_status_and_the_update_date_only() {
        ContactMessage read = submit("Nom", "a@example.com", "Sujet", "Message").moveTo(ContactStatus.READ, LATER);

        assertThat(read.status()).isEqualTo(ContactStatus.READ);
        assertThat(read.createdAt()).isEqualTo(RECEIVED);
        assertThat(read.updatedAt()).isEqualTo(LATER);
        assertThat(read.subject()).isEqualTo("Sujet");
    }

    @Test
    void refuses_to_move_back_or_to_stay_in_place() {
        ContactMessage processed = submit("Nom", "a@example.com", "Sujet", "Message")
            .moveTo(ContactStatus.PROCESSED, LATER);

        assertThatThrownBy(() -> processed.moveTo(ContactStatus.READ, LATER))
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_CONTACT_MESSAGE_TRANSITION));
        assertThatThrownBy(() -> processed.moveTo(ContactStatus.PROCESSED, LATER))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void is_never_updated_before_it_was_received() {
        assertThatThrownBy(() -> new ContactMessage(1L, "Nom", "a@example.com", "Sujet", "Message",
            ContactStatus.READ, LATER, RECEIVED))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static ContactMessage submit(String name, String email, String subject, String message) {
        return ContactMessage.submit(name, email, subject, message, RECEIVED);
    }
}
