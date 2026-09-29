package com.scalke.portfolio.backend.contact.domain.model;

import java.util.Objects;

/**
 * Un message de contact vient d'être enregistré (D-CJ). Publié dans la transaction de l'enregistrement, traité
 * seulement après sa validation : un message annulé ne déclenche rien.
 */
public record ContactMessageReceived(ContactMessage message) {

    public ContactMessageReceived {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(message.id(), "message.id");
    }
}
