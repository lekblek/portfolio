package com.scalke.portfolio.backend.publication.domain.model;

import java.util.Set;

/**
 * Ce que l'administrateur saisit et modifie d'une publication (D-CU) : tout sauf le type (fixé à la création), le
 * slug (règle propre, {@link Publication#edit}), le statut et ses dates (cycle éditorial,
 * {@link Publication#transitionTo}). Espaces de début et de fin retirés ; champs SEO vides → {@code null} ; tags
 * absents → aucun. Les bornes sont vérifiées par {@link Publication}.
 */
public record PublicationContent(
    String title,
    String summary,
    String contentMarkdown,
    boolean featured,
    Long categoryId,
    Set<Long> tagIds,
    Long coverMediaId,
    String seoTitle,
    String seoDescription
) {

    public PublicationContent {
        title = title == null ? null : title.strip();
        summary = summary == null ? null : summary.strip();
        tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
        seoTitle = seoTitle == null || seoTitle.isBlank() ? null : seoTitle.strip();
        seoDescription = seoDescription == null || seoDescription.isBlank() ? null : seoDescription.strip();
    }
}
