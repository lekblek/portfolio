package com.scalke.portfolio.backend.publication.web.dto;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;

import java.time.Instant;

/**
 * Détail public d'une publication ({@code GET /api/public/publications/{slug}}, D-AJ).
 * <p>
 * {@code seoTitle} et {@code seoDescription} sont publics : le rendu serveur Angular en a besoin pour
 * les balises {@code <title>} et {@code <meta>} ; {@code null} signifie « utiliser le titre / le résumé ».
 * Champs optionnels présents avec {@code null} (C10).
 */
public record PublicationResponse(
    PublicationType type,
    String title,
    String slug,
    String summary,
    String contentMarkdown,
    Instant publishedAt,
    boolean featured,
    int readingTimeMinutes,
    String seoTitle,
    String seoDescription
) {

    public static PublicationResponse from(Publication publication) {
        return new PublicationResponse(
            publication.type(),
            publication.title(),
            publication.slug(),
            publication.summary(),
            publication.contentMarkdown(),
            publication.publishedAt(),
            publication.featured(),
            publication.readingTimeMinutes(),
            publication.seoTitle(),
            publication.seoDescription());
    }
}
