package com.scalke.portfolio.backend.contact.domain.port;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;

/**
 * Prévient l'administrateur qu'un message de contact a été reçu (D-CI). Implémentation V1 : courriel par SMTP.
 * Peut échouer (exception non vérifiée) : l'appelant décide qu'un échec n'annule rien (D14).
 */
public interface ContactNotificationSender {

    void messageReceived(ContactMessage message);
}
