package com.scalke.portfolio.backend.series.domain.model;

/**
 * Place d'un article parmi les chapitres visibles de sa série (D-BM) : {@code position} est sa position
 * publique (rang à partir de 1, D-BG) sur {@code chapterCount} chapitres, ce qui donne la progression.
 * {@code previousPublicationId} vaut {@code null} au premier chapitre, {@code nextPublicationId} au dernier.
 */
public record ChapterNavigation(
    int position,
    int chapterCount,
    Long previousPublicationId,
    Long nextPublicationId
) {

    public ChapterNavigation {
        if (position < 1 || position > chapterCount) {
            throw new IllegalArgumentException("position " + position + " outside 1.." + chapterCount);
        }
        if ((previousPublicationId == null) != (position == 1)) {
            throw new IllegalArgumentException("only the first chapter has no previous chapter");
        }
        if ((nextPublicationId == null) != (position == chapterCount)) {
            throw new IllegalArgumentException("only the last chapter has no next chapter");
        }
    }
}
