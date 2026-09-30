package com.scalke.portfolio.backend.publication.web.dto;

import com.scalke.portfolio.backend.publication.application.usecase.AdminPublication;
import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Ligne de la liste d'administration (D-CU), sans le contenu.
 */
public record AdminPublicationSummaryResponse(
    Long id,
    PublicationType type,
    String title,
    String slug,
    PublicationStatus status,
    @Nullable Instant publishedAt,
    boolean featured,
    Instant updatedAt
) {

    public static AdminPublicationSummaryResponse from(AdminPublication view) {
        Publication publication = view.publication();
        return new AdminPublicationSummaryResponse(
            publication.id(),
            publication.type(),
            publication.title(),
            publication.slug().value(),
            view.status(),
            publication.publishedAt(),
            publication.featured(),
            publication.updatedAt());
    }
}
