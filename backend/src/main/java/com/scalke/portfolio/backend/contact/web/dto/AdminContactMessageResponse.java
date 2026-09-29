package com.scalke.portfolio.backend.contact.web.dto;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;

import java.time.Instant;

/**
 * Message de contact vu par l'administration (D-CZ), avec son texte.
 */
public record AdminContactMessageResponse(
    Long id,
    String name,
    String email,
    String subject,
    String message,
    ContactStatus status,
    Instant createdAt,
    Instant updatedAt
) {

    public static AdminContactMessageResponse from(ContactMessage message) {
        return new AdminContactMessageResponse(message.id(), message.name(), message.email(), message.subject(),
            message.message(), message.status(), message.createdAt(), message.updatedAt());
    }
}
