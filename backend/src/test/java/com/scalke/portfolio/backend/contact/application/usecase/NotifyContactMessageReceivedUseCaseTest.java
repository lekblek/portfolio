package com.scalke.portfolio.backend.contact.application.usecase;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.model.ContactMessageReceived;
import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NotifyContactMessageReceivedUseCaseTest {

    private static final ContactMessageReceived RECEIVED = new ContactMessageReceived(new ContactMessage(7L,
        "Camille", "camille@example.com", "Mission", "Bonjour.", ContactStatus.NEW,
        Instant.parse("2026-06-15T10:00:00Z"), Instant.parse("2026-06-15T10:00:00Z")));

    @Test
    void notifies_the_administrator_of_the_received_message() {
        List<ContactMessage> notified = new ArrayList<>();

        new NotifyContactMessageReceivedUseCase(notified::add).execute(RECEIVED);

        assertThat(notified).containsExactly(RECEIVED.message());
    }

    /**
     * D14 : un échec d'envoi est journalisé, jamais propagé à qui a enregistré le message.
     */
    @Test
    void swallows_a_failed_notification() {
        NotifyContactMessageReceivedUseCase useCase = new NotifyContactMessageReceivedUseCase(message -> {
            throw new IllegalStateException("SMTP indisponible");
        });

        assertThatCode(() -> useCase.execute(RECEIVED)).doesNotThrowAnyException();
    }
}
