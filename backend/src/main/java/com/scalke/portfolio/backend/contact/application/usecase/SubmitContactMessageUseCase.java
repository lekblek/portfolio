package com.scalke.portfolio.backend.contact.application.usecase;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.port.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Enregistre un message reçu par le formulaire de contact, au statut {@code NEW} ({@code 01} §13, D-CG).
 * <p>
 * Pas de route avant l'étape 48 : le flux de {@code 01} §13 place le piège à robots (honeypot) et la
 * limitation de débit avant la sauvegarde, et une route publique en écriture sans eux serait ouverte au spam.
 * La notification par e-mail (étape 31) aura lieu après la validation de la transaction : un échec SMTP ne
 * doit jamais annuler le message enregistré (D14).
 */
@Service
@RequiredArgsConstructor
public class SubmitContactMessageUseCase {

    private final ContactMessageRepository contactMessageRepository;
    private final Clock clock;

    @Transactional
    public ContactMessage execute(ContactMessageSubmission submission) {
        return contactMessageRepository.create(ContactMessage.submit(
            submission.name(), submission.email(), submission.subject(), submission.message(), clock.instant()));
    }
}
