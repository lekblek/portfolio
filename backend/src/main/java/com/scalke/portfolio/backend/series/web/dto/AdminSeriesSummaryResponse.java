package com.scalke.portfolio.backend.series.web.dto;

import com.scalke.portfolio.backend.series.domain.model.Series;

/**
 * Ligne de la liste d'administration des séries (D-CV).
 */
public record AdminSeriesSummaryResponse(Long id, String title, String slug, int chapterCount) {

    public static AdminSeriesSummaryResponse from(Series series) {
        return new AdminSeriesSummaryResponse(series.id(), series.title(), series.slug().value(), series.items().size());
    }
}
