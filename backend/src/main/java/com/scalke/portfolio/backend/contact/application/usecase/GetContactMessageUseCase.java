package com.scalke.portfolio.backend.contact.application.usecase;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.port.ContactMessageRepository;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Un message de contact, pour l'administration (D-CZ). Le lire ne change pas son statut : c'est l'administrateur qui le
 * fait avancer, explicitement.
 */
@Service
@RequiredArgsConstructor
public class GetContactMessageUseCase {

    private final ContactMessageRepository contactMessageRepository;

    @Transactional(readOnly = true)
    public ContactMessage execute(Long id) {
        return contactMessageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Message introuvable."));
    }
}
