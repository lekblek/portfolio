package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.series.domain.model.Series;

/**
 * Contexte de série d'un article visible (D-BL, D-BM) : sa série, sa position publique sur
 * {@code chapterCount} chapitres visibles, et ses voisins visibles ({@code null} au premier et au dernier).
 */
public record SeriesNavigation(
    Series series,
    int position,
    int chapterCount,
    SeriesChapter previous,
    SeriesChapter next
) {
}
