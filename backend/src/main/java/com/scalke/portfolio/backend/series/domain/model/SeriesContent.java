package com.scalke.portfolio.backend.series.domain.model;

/**
 * Ce que l'administrateur saisit d'une série (D-CV) : tout sauf le slug (règle propre, {@link Series#edit}) et les
 * chapitres (remplacés d'un bloc, {@link Series#withChapters}). Titre sans espaces de début et de fin ; les bornes
 * sont vérifiées par {@link Series}.
 */
public record SeriesContent(String title, String descriptionMarkdown, Long coverMediaId) {

    public SeriesContent {
        title = title == null ? null : title.strip();
    }
}
