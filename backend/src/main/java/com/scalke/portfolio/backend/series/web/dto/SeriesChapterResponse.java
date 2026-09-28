package com.scalke.portfolio.backend.series.web.dto;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.series.application.usecase.SeriesChapter;

import java.time.Instant;

/**
 * Entrée de la table des matières d'une série. {@code position} est la position publique (rang parmi les
 * articles visibles, D-BG) ; {@code slug} mène à {@code /api/public/publications/{slug}}.
 */
public record SeriesChapterResponse(
    int position,
    String title,
    String slug,
    String summary,
    Instant publishedAt,
    int readingTimeMinutes
) {

    public static SeriesChapterResponse from(SeriesChapter chapter) {
        Publication article = chapter.publication();
        return new SeriesChapterResponse(
            chapter.position(),
            article.title(),
            article.slug().value(),
            article.summary(),
            article.publishedAt(),
            article.readingTimeMinutes());
    }
}
