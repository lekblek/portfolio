package com.scalke.portfolio.backend.contact.application.usecase;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Notification après enregistrement (D14, D-CJ), sur un vrai serveur SMTP en mémoire. <strong>Sans</strong>
 * transaction de test : la notification n'a lieu qu'après une validation réelle, que la transaction d'un test
 * annulerait. Chaque test supprime les messages qu'il crée.
 */
class ContactMessageNotificationIT extends AbstractIntegrationTest {

    private static final ContactMessageSubmission SUBMISSION = new ContactMessageSubmission(
        "Camille Martin", "camille@example.com", "Proposition de mission", "Bonjour,\nUne mission Spring Boot ?");

    @Autowired
    SubmitContactMessageUseCase submitContactMessageUseCase;

    @Autowired
    GreenMail greenMail;

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    TransactionTemplate transactionTemplate;

    @BeforeEach
    void givenAnEmptyMailbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    @AfterEach
    void removeTheMessages() {
        jdbcClient.sql("DELETE FROM contact_message").update();
    }

    @Test
    void notifies_the_administrator_once_the_message_is_recorded() throws Exception {
        submitContactMessageUseCase.execute(SUBMISSION);

        assertThat(greenMail.waitForIncomingEmail(5_000, 1)).isTrue();
        MimeMessage mail = greenMail.getReceivedMessages()[0];
        assertThat(mail.getSubject()).isEqualTo("[Portfolio] Proposition de mission");
        assertThat(mail.getRecipients(Message.RecipientType.TO)).extracting(Object::toString)
            .containsExactly("admin@localhost");
        assertThat(mail.getReplyTo()).extracting(Object::toString).containsExactly("camille@example.com");
        assertThat(GreenMailUtil.getBody(mail)).contains("Camille Martin");
    }

    /**
     * Un message dont l'enregistrement est annulé n'existe pas : personne n'est prévenu.
     */
    @Test
    void notifies_nobody_when_the_recording_is_rolled_back() {
        transactionTemplate.executeWithoutResult(status -> {
            submitContactMessageUseCase.execute(SUBMISSION);
            status.setRollbackOnly();
        });

        assertThat(greenMail.waitForIncomingEmail(1_000, 1)).isFalse();
        assertThat(count()).isZero();
    }

    /**
     * D14 : serveur SMTP injoignable, le message reste enregistré et l'appelant ne voit aucune erreur.
     */
    @Test
    void keeps_the_message_when_the_mail_server_is_down() {
        greenMail.stop();
        try {
            submitContactMessageUseCase.execute(SUBMISSION);
        } finally {
            greenMail.start();
        }

        assertThat(count()).isEqualTo(1);
    }

    private long count() {
        return jdbcClient.sql("SELECT count(*) FROM contact_message").query(Long.class).single();
    }
}
