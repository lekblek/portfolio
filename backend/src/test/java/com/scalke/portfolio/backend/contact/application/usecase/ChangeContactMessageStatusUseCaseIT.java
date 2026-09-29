package com.scalke.portfolio.backend.contact.application.usecase;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;
import com.scalke.portfolio.backend.contact.domain.port.ContactMessageRepository;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cycle de traitement d'un message sans HTTP (route d'administration à l'étape 36, D-AU, D-CH).
 */
@Transactional
class ChangeContactMessageStatusUseCaseIT extends AbstractIntegrationTest {

    private static final Instant RECEIVED = NOW.minus(Duration.ofDays(1));

    @Autowired
    ChangeContactMessageStatusUseCase changeContactMessageStatusUseCase;

    @Autowired
    ContactMessageRepository contactMessageRepository;

    @Autowired
    EntityManager entityManager;

    private Long id;

    @BeforeEach
    void givenANewMessage() {
        id = contactMessageRepository.create(
            ContactMessage.submit("Camille", "camille@example.com", "Mission", "Bonjour.", RECEIVED)).id();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void writes_the_new_status_and_the_update_date() {
        changeContactMessageStatusUseCase.execute(id, ContactStatus.READ);
        entityManager.flush();
        entityManager.clear();

        ContactMessage stored = contactMessageRepository.findById(id).orElseThrow();
        assertThat(stored.status()).isEqualTo(ContactStatus.READ);
        assertThat(stored.createdAt()).isEqualTo(RECEIVED);
        assertThat(stored.updatedAt()).isEqualTo(NOW);
    }

    /**
     * Un message indésirable s'archive directement, sans passer par la lecture et le traitement.
     */
    @Test
    void may_skip_steps_of_the_cycle() {
        assertThat(changeContactMessageStatusUseCase.execute(id, ContactStatus.ARCHIVED).status())
            .isEqualTo(ContactStatus.ARCHIVED);
    }

    @Test
    void refuses_to_go_back_and_leaves_the_message_unchanged() {
        changeContactMessageStatusUseCase.execute(id, ContactStatus.PROCESSED);
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> changeContactMessageStatusUseCase.execute(id, ContactStatus.READ))
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_CONTACT_MESSAGE_TRANSITION));
        assertThat(contactMessageRepository.findById(id).orElseThrow().status()).isEqualTo(ContactStatus.PROCESSED);
    }

    @Test
    void an_unknown_message_is_not_found() {
        assertThatThrownBy(() -> changeContactMessageStatusUseCase.execute(999_999L, ContactStatus.READ))
            .isInstanceOfSatisfying(ResourceNotFoundException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
