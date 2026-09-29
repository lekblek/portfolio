package com.scalke.portfolio.backend.contact.infrastructure.mail;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailContactNotificationSenderTest {

    private static final ContactMessage MESSAGE = new ContactMessage(7L, "Camille Martin", "camille@example.com",
        "Mission\r\nBcc: tous@example.com", "Bonjour,\n\nUne mission Spring Boot ?", ContactStatus.NEW,
        Instant.parse("2026-06-15T10:00:00Z"), Instant.parse("2026-06-15T10:00:00Z"));

    private final EmailContactNotificationSender sender = new EmailContactNotificationSender(null,
        new ContactNotificationProperties("portfolio@scalke.dev", "admin@scalke.dev"));

    @Test
    void writes_to_the_administrator_and_lets_them_reply_to_the_visitor() {
        SimpleMailMessage mail = sender.toMail(MESSAGE);

        assertThat(mail.getFrom()).isEqualTo("portfolio@scalke.dev");
        assertThat(mail.getTo()).containsExactly("admin@scalke.dev");
        assertThat(mail.getReplyTo()).isEqualTo("camille@example.com");
        assertThat(mail.getText())
            .contains("De : Camille Martin <camille@example.com>")
            .contains("Reçu le : 2026-06-15T10:00:00Z")
            .contains("Bonjour,\n\nUne mission Spring Boot ?");
    }

    /**
     * Un saut de ligne dans le sujet du visiteur n'ajoute aucun en-tête au courriel.
     */
    @Test
    void keeps_the_subject_on_one_line() {
        assertThat(sender.toMail(MESSAGE).getSubject()).isEqualTo("[Portfolio] Mission Bcc: tous@example.com");
    }

    /**
     * D-DD : sans hôte SMTP, le démarrage échoue en nommant la variable, au lieu de perdre chaque notification.
     */
    @Test
    void refuses_a_mail_server_without_host() {
        ContactNotificationProperties properties = new ContactNotificationProperties("a@b.dev", "c@d.dev");

        assertThatThrownBy(() -> new EmailContactNotificationSender(new JavaMailSenderImpl(), properties))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("MAIL_HOST");
        JavaMailSenderImpl configured = new JavaMailSenderImpl();
        configured.setHost("smtp.example.com");
        assertThat(new EmailContactNotificationSender(configured, properties)).isNotNull();
    }
}
