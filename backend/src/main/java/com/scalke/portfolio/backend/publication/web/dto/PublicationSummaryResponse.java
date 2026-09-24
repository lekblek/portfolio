package com.scalke.portfolio.backend.publication.web.dto;

import com.scalke.portfolio.backend.publication.application.usecase.VisiblePublication;
import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;

import java.time.Instant;
import java.util.List;

/**
 * Publication dans une liste publique (D-AJ, D-AP). Ni identifiant, ni statut (une publication listée est
 * visible ; une planifiée échue ne se distingue pas d'une publiée), ni dates d'audit.
 * {@code category} vaut {@code null} si la publication n'est pas classée ; {@code tags} vaut {@code []}.
 */
public record PublicationSummaryResponse(
    PublicationType type,
    String title,
    String slug,
    String summary,
    Instant publishedAt,
    boolean featured,
    int readingTimeMinutes,
    TaxonomyTermResponse category,
    List<TaxonomyTermResponse> tags
) {

    public static PublicationSummaryResponse from(VisiblePublication visible) {
        Publication publication = visible.publication();
        return new PublicationSummaryResponse(
            publication.type(),
            publication.title(),
            publication.slug(),
            publication.summary(),
            publication.publishedAt(),
            publication.featured(),
            publication.readingTimeMinutes(),
            TaxonomyTermResponse.from(visible.category()),
            TaxonomyTermResponse.from(visible.tags()));
    }
}
