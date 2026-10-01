package com.scalke.portfolio.backend.contact.application.usecase;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;
import com.scalke.portfolio.backend.contact.domain.port.ContactMessageRepository;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enregistrement d'un message sans HTTP (D-CG), à l'horloge fixe des tests ; la route : {@code PublicContactMessageIT}.
 */
@Transactional
class SubmitContactMessageUseCaseIT extends AbstractIntegrationTest {

    @Autowired
    SubmitContactMessageUseCase submitContactMessageUseCase;

    @Autowired
    ContactMessageRepository contactMessageRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void records_a_new_message_received_now() {
        ContactMessage submitted = submitContactMessageUseCase.execute(new ContactMessageSubmission(
            " Camille Martin ", "camille.martin@example.com", "Proposition de mission", "Bonjour,\n\nUne mission ?"), "203.0.113.10");
        entityManager.flush();
        entityManager.clear();

        ContactMessage stored = contactMessageRepository.findById(submitted.id()).orElseThrow();
        assertThat(stored).isEqualTo(submitted);
        assertThat(stored.name()).isEqualTo("Camille Martin");
        assertThat(stored.message()).isEqualTo("Bonjour,\n\nUne mission ?");
        assertThat(stored.status()).isEqualTo(ContactStatus.NEW);
        assertThat(stored.createdAt()).isEqualTo(NOW);
        assertThat(stored.updatedAt()).isEqualTo(NOW);
    }

    /**
     * {@code 02} §23 : aucune donnée de la requête (adresse IP, navigateur) n'est conservée avec le message.
     */
    @Test
    void stores_nothing_but_the_message_itself() {
        List<String> columns = jdbcClient.sql("""
                    SELECT column_name FROM information_schema.columns
                     WHERE table_name = 'contact_message' ORDER BY ordinal_position
                    """)
            .query(String.class)
            .list();

        assertThat(columns)
            .containsExactly("id", "name", "email", "subject", "message", "status", "created_at", "updated_at");
    }
}
