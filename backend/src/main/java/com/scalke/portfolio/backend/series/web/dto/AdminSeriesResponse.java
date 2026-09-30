package com.scalke.portfolio.backend.series.web.dto;

import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.series.application.usecase.AdminSeries;
import com.scalke.portfolio.backend.series.domain.model.Series;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Série vue par l'administration (D-CV) : saisie, couverture par identifiant, slug verrouillé ou non, et tous les
 * chapitres dans l'ordre, avec le statut observable de leur article.
 */
public record AdminSeriesResponse(
    Long id,
    String title,
    String slug,
    boolean slugLocked,
    String descriptionMarkdown,
    @Nullable Long coverMediaId,
    List<Chapter> chapters
) {

    public static AdminSeriesResponse from(AdminSeries view) {
        Series series = view.series();
        return new AdminSeriesResponse(
            series.id(),
            series.title(),
            series.slug().value(),
            view.slugLocked(),
            series.descriptionMarkdown(),
            series.coverMediaId(),
            view.chapters().stream().map(Chapter::from).toList());
    }

    public record Chapter(int position, Long publicationId, String title, String slug, PublicationStatus status) {

        static Chapter from(AdminSeries.Chapter chapter) {
            return new Chapter(chapter.position(), chapter.publication().id(), chapter.publication().title(),
                chapter.publication().slug().value(), chapter.status());
        }
    }
}
