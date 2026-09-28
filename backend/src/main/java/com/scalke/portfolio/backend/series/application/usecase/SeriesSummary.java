package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.media.application.query.PublicImage;
import com.scalke.portfolio.backend.series.domain.model.Series;

/**
 * Série publique dans une liste : {@code chapterCount} compte ses seuls articles visibles (D-BG) ; {@code cover}
 * est sa couverture sous forme publique, {@code null} si aucune (D-BY).
 */
public record SeriesSummary(Series series, int chapterCount, PublicImage cover) {
}
