package com.scalke.portfolio.backend.series.web.dto;

import com.scalke.portfolio.backend.series.application.usecase.SeriesChapter;

/**
 * Lien vers un chapitre voisin : position publique, titre et slug de l'article ({@code null} s'il n'y en a pas).
 */
public record ChapterLinkResponse(int position, String title, String slug) {

    public static ChapterLinkResponse from(SeriesChapter chapter) {
        if (chapter == null) {
            return null;
        }
        return new ChapterLinkResponse(
            chapter.position(),
            chapter.publication().title(),
            chapter.publication().slug().value());
    }
}
