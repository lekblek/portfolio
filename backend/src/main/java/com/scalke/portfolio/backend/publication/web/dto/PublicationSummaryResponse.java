package com.scalke.portfolio.backend.publication.web.dto;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;

import java.time.Instant;

/**
 * Publication dans une liste publique (D-AJ). Ni identifiant, ni statut (une publication listée est
 * visible ; une planifiée échue ne se distingue pas d'une publiée), ni dates d'audit.
 */
public record PublicationSummaryResponse(
    PublicationType type,
    String title,
    String slug,
    String summary,
    Instant publishedAt,
    boolean featured,
    int readingTimeMinutes
) {

    public static PublicationSummaryResponse from(Publication publication) {
        return new PublicationSummaryResponse(
            publication.type(),
            publication.title(),
            publication.slug(),
            publication.summary(),
            publication.publishedAt(),
            publication.featured(),
            publication.readingTimeMinutes());
    }
}
