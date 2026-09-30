package com.scalke.portfolio.backend.search.web.dto;

import com.scalke.portfolio.backend.search.application.usecase.SearchResult;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Résultat d'une recherche publique (D-CE) : de quoi afficher le contenu trouvé et construire son lien
 * ({@code type} et {@code slug}). {@code summary} est le résumé d'une publication ou la description courte d'un
 * projet ; {@code publishedAt} vaut {@code null} pour un projet. Ni identifiant, ni pertinence.
 */
public record SearchResultResponse(
    SearchResult.Type type,
    String title,
    String slug,
    String summary,
    @Nullable Instant publishedAt
) {

    public static SearchResultResponse from(SearchResult result) {
        return new SearchResultResponse(
            result.type(),
            result.title(),
            result.slug().value(),
            result.summary(),
            result.publishedAt());
    }
}
