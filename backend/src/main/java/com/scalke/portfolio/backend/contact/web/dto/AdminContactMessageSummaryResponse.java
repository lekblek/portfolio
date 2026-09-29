package com.scalke.portfolio.backend.contact.web.dto;

import com.scalke.portfolio.backend.contact.domain.model.ContactMessage;
import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;

import java.time.Instant;

/**
 * Ligne de la boîte de réception (D-CZ), sans le texte du message.
 */
public record AdminContactMessageSummaryResponse(
    Long id,
    String name,
    String email,
    String subject,
    ContactStatus status,
    Instant createdAt
) {

    public static AdminContactMessageSummaryResponse from(ContactMessage message) {
        return new AdminContactMessageSummaryResponse(message.id(), message.name(), message.email(),
            message.subject(), message.status(), message.createdAt());
    }
}
