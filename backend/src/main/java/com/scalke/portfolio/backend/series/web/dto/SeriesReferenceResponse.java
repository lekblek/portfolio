package com.scalke.portfolio.backend.series.web.dto;

import com.scalke.portfolio.backend.series.domain.model.Series;

/**
 * Série citée depuis un article : de quoi afficher son titre et mener à {@code /api/public/series/{slug}}.
 */
public record SeriesReferenceResponse(String title, String slug) {

    public static SeriesReferenceResponse from(Series series) {
        return new SeriesReferenceResponse(series.title(), series.slug().value());
    }
}
