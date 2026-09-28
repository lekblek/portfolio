package com.scalke.portfolio.backend.series.web.dto;

import com.scalke.portfolio.backend.media.application.query.PublicImage;
import com.scalke.portfolio.backend.series.application.usecase.SeriesSummary;
import com.scalke.portfolio.backend.series.domain.model.Series;

/**
 * Série dans une liste publique (D-BI) : {@code chapterCount} compte ses articles visibles (au moins 1).
 */
public record SeriesSummaryResponse(
    String title,
    String slug,
    String descriptionMarkdown,
    PublicImage cover,
    int chapterCount
) {

    public static SeriesSummaryResponse from(SeriesSummary summary) {
        Series series = summary.series();
        return new SeriesSummaryResponse(
            series.title(),
            series.slug().value(),
            series.descriptionMarkdown(),
            summary.cover(),
            summary.chapterCount());
    }
}
