package com.scalke.portfolio.backend.contact.application.usecase;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.model.ContactMessageReceived;
import com.scalke.portfolio.backend.contact.domain.port.ContactMessageRepository;
import com.scalke.portfolio.backend.shared.domain.model.SlidingWindowLimit;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Enregistre un message reçu par le formulaire de contact, au statut {@code NEW} ({@code 01} §13, D-CG).
 * <p>
 * Limitation de débit avant la sauvegarde (D-EJ) : {@value #MAX_MESSAGES} messages par adresse du client sur une
 * heure glissante ; au-delà, 429 {@code TOO_MANY_CONTACT_MESSAGES} avec le délai d'attente, rien n'est enregistré.
 * L'adresse ne sert qu'à cette limite, en mémoire : elle n'est jamais enregistrée avec le message ({@code 01} §13).
 * L'administrateur est prévenu après la validation de la transaction ({@link NotifyContactMessageReceivedUseCase},
 * D-CJ) : un échec SMTP n'annule jamais le message enregistré (D14).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmitContactMessageUseCase {

    static final int MAX_MESSAGES = 5;
    static final Duration WINDOW = Duration.ofHours(1);

    private final ContactMessageRepository contactMessageRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final SlidingWindowLimit messagesBySource = new SlidingWindowLimit(MAX_MESSAGES, WINDOW);

    /**
     * @param source adresse du client, clé de la limitation de débit
     */
    @Transactional
    public ContactMessage execute(ContactMessageSubmission submission, String source) {
        Instant now = clock.instant();
        Optional<Duration> retryAfter = messagesBySource.retryAfter(source, now);
        if (retryAfter.isPresent()) {
            log.warn("Message de contact refusé depuis {} : trop de messages, nouvel essai dans {} s", source,
                retryAfter.get().toSeconds());
            throw new TooManyRequestsException(ErrorCode.TOO_MANY_CONTACT_MESSAGES,
                "Trop de messages envoyés : réessayer plus tard.", retryAfter.get());
        }
        messagesBySource.record(source, now);
        ContactMessage saved = contactMessageRepository.create(ContactMessage.submit(
            submission.name(), submission.email(), submission.subject(), submission.message(), now));
        events.publishEvent(new ContactMessageReceived(saved));
        return saved;
    }
}
