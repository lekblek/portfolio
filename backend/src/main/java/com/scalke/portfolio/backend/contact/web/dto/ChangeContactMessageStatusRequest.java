package com.scalke.portfolio.backend.contact.web.dto;

import com.scalke.portfolio.backend.contact.domain.model.ContactStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Nouveau statut d'un message (invariant 29 : en avant seulement, D-CH).
 */
public record ChangeContactMessageStatusRequest(@NotNull ContactStatus status) {
}
