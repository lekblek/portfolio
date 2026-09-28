package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.series.domain.model.Series;

import java.util.List;

/**
 * Série publique et sa table des matières : ses articles visibles, dans l'ordre (au moins un, D-BG).
 */
public record VisibleSeries(Series series, List<SeriesChapter> chapters) {

    public VisibleSeries {
        chapters = List.copyOf(chapters);
    }
}
