package com.scalke.portfolio.backend.contact.application.usecase;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.model.ContactMessageReceived;
import com.scalke.portfolio.backend.contact.domain.port.ContactMessageRepository;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Limitation de débit des messages de contact (D-EJ), à l'horloge fixe.
 */
class SubmitContactMessageUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-01T09:00:00Z");
    private static final ContactMessageSubmission SUBMISSION = new ContactMessageSubmission(
        "Camille Martin", "camille@example.com", "Proposition", "Bonjour.");

    private final ContactMessageRepository repository = mock(ContactMessageRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final SubmitContactMessageUseCase useCase =
        new SubmitContactMessageUseCase(repository, events, Clock.fixed(NOW, ZoneOffset.UTC));

    /**
     * Ce que rend le dépôt : le message avec un identifiant, que l'événement de réception exige.
     */
    private static ContactMessage saved(ContactMessage message) {
        return new ContactMessage(1L, message.name(), message.email(), message.subject(), message.message(),
            message.status(), message.createdAt(), message.updatedAt());
    }

    @Test
    void refuses_the_sixth_message_of_the_hour_from_the_same_address_and_records_nothing() {
        given(repository.create(any())).willAnswer(call -> saved(call.getArgument(0)));
        for (int i = 0; i < SubmitContactMessageUseCase.MAX_MESSAGES; i++) {
            useCase.execute(SUBMISSION, "203.0.113.1");
        }

        assertThatThrownBy(() -> useCase.execute(SUBMISSION, "203.0.113.1"))
            .isInstanceOfSatisfying(TooManyRequestsException.class, refused -> {
                assertThat(refused.errorCode()).isEqualTo(ErrorCode.TOO_MANY_CONTACT_MESSAGES);
                assertThat(refused.retryAfter()).isEqualTo(Duration.ofHours(1));
            });
        verify(repository, times(SubmitContactMessageUseCase.MAX_MESSAGES)).create(any());
        verify(events, times(SubmitContactMessageUseCase.MAX_MESSAGES)).publishEvent(any(ContactMessageReceived.class));
    }

    @Test
    void counts_each_address_separately() {
        given(repository.create(any())).willAnswer(call -> saved(call.getArgument(0)));
        for (int i = 0; i < SubmitContactMessageUseCase.MAX_MESSAGES; i++) {
            useCase.execute(SUBMISSION, "203.0.113.1");
        }

        ContactMessage accepted = useCase.execute(SUBMISSION, "203.0.113.2");

        assertThat(accepted.createdAt()).isEqualTo(NOW);
    }
}
