package com.scalke.portfolio.backend.series.web.dto;

import com.scalke.portfolio.backend.media.application.query.PublicImage;
import com.scalke.portfolio.backend.series.application.usecase.VisibleSeries;
import com.scalke.portfolio.backend.series.domain.model.Series;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Détail public d'une série (D-BI) : table des matières limitée aux articles visibles, dans l'ordre. Ni
 * identifiant, ni position interne.
 */
public record SeriesResponse(
    String title,
    String slug,
    String descriptionMarkdown,
    @Nullable PublicImage cover,
    List<SeriesChapterResponse> chapters
) {

    public static SeriesResponse from(VisibleSeries visible) {
        Series series = visible.series();
        return new SeriesResponse(
            series.title(),
            series.slug().value(),
            series.descriptionMarkdown(),
            visible.cover(),
            visible.chapters().stream().map(SeriesChapterResponse::from).toList());
    }
}
