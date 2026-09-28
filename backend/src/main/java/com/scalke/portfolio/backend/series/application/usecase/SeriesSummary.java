package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.series.domain.model.Series;

/**
 * Série publique dans une liste : {@code chapterCount} compte ses seuls articles visibles (D-BG).
 */
public record SeriesSummary(Series series, int chapterCount) {
}
