package com.scalke.portfolio.backend.series.web.dto;

import com.scalke.portfolio.backend.series.application.usecase.SeriesNavigation;
import org.jspecify.annotations.Nullable;

/**
 * Contexte de série d'un article (D-BL) : {@code position} sur {@code chapterCount} donne la progression ;
 * {@code previous} et {@code next} valent {@code null} au premier et au dernier chapitre visible (C10).
 */
public record SeriesNavigationResponse(
    SeriesReferenceResponse series,
    int position,
    int chapterCount,
    @Nullable ChapterLinkResponse previous,
    @Nullable ChapterLinkResponse next
) {

    public static SeriesNavigationResponse from(SeriesNavigation navigation) {
        return new SeriesNavigationResponse(
            SeriesReferenceResponse.from(navigation.series()),
            navigation.position(),
            navigation.chapterCount(),
            ChapterLinkResponse.from(navigation.previous()),
            ChapterLinkResponse.from(navigation.next()));
    }
}
