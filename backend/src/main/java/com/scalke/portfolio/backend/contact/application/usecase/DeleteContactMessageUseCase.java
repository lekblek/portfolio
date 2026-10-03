package com.scalke.portfolio.backend.contact.application.usecase;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.port.ContactMessageRepository;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suppression définitive d'un message de contact (F30, D-EX) : effacement des données personnelles à la demande de la
 * personne, quel que soit le statut du message. L'archivage, lui, range un message sans l'effacer (D-CZ). La
 * notification déjà envoyée à l'administrateur n'est pas concernée.
 */
@Service
@RequiredArgsConstructor
public class DeleteContactMessageUseCase {

    private final ContactMessageRepository contactMessageRepository;

    @Transactional
    public void execute(Long id) {
        ContactMessage message = contactMessageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Message introuvable."));
        contactMessageRepository.delete(message);
    }
}
