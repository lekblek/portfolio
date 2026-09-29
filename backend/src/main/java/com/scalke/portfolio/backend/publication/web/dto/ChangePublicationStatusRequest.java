package com.scalke.portfolio.backend.publication.web.dto;

import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Changement de statut ({@code 05} §17, D-AU) : {@code publishedAt} pour une planification seulement.
 */
public record ChangePublicationStatusRequest(@NotNull PublicationStatus status, Instant publishedAt) {
}
