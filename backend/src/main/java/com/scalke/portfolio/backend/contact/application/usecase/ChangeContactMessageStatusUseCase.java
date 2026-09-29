package com.scalke.portfolio.backend.contact.application.usecase;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;
import com.scalke.portfolio.backend.contact.domain.port.ContactMessageRepository;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Fait avancer un message dans son cycle de traitement (invariant 29, D-CH).
 * <p>
 * Opération d'administration, exposée par {@code POST /api/admin/contact-messages/{id}/status} (D-AU, D-CZ). Seul appelant de {@code ContactMessageRepository.updateStatus} : c'est ce qui garantit l'invariant 29,
 * qu'aucune contrainte SQL ne peut vérifier. Message inconnu → 404 ; transition refusée → 409
 * {@code INVALID_CONTACT_MESSAGE_TRANSITION}.
 */
@Service
@RequiredArgsConstructor
public class ChangeContactMessageStatusUseCase {

    private final ContactMessageRepository contactMessageRepository;
    private final Clock clock;

    @Transactional
    public ContactMessage execute(Long messageId, ContactStatus target) {
        ContactMessage message = contactMessageRepository.findById(messageId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Message introuvable."));
        return contactMessageRepository.updateStatus(message.moveTo(target, clock.instant()));
    }
}
