package com.scalke.portfolio.backend.publication.web.dto;

import com.scalke.portfolio.backend.publication.application.usecase.AdminPublication;
import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;

import java.time.Instant;
import java.util.List;

/**
 * Publication vue par l'administration (D-CU) : toute la saisie, les termes et la couverture par identifiant, le
 * statut observable et si le slug peut encore changer.
 */
public record AdminPublicationResponse(
    Long id,
    PublicationType type,
    String title,
    String slug,
    boolean slugLocked,
    String summary,
    String contentMarkdown,
    PublicationStatus status,
    Instant publishedAt,
    boolean featured,
    Long categoryId,
    List<Long> tagIds,
    Long coverMediaId,
    String seoTitle,
    String seoDescription,
    Instant createdAt,
    Instant updatedAt
) {

    public static AdminPublicationResponse from(AdminPublication view) {
        Publication publication = view.publication();
        return new AdminPublicationResponse(
            publication.id(),
            publication.type(),
            publication.title(),
            publication.slug().value(),
            view.slugLocked(),
            publication.summary(),
            publication.contentMarkdown(),
            view.status(),
            publication.publishedAt(),
            publication.featured(),
            publication.categoryId(),
            publication.tagIds().stream().sorted().toList(),
            publication.coverMediaId(),
            publication.seoTitle(),
            publication.seoDescription(),
            publication.createdAt(),
            publication.updatedAt());
    }
}
