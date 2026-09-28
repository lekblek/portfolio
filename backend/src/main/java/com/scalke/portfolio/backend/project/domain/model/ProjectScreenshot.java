package com.scalke.portfolio.backend.project.domain.model;

import java.util.Comparator;
import java.util.Objects;

/**
 * Capture d'écran d'un projet ({@code 02} §12) : référence vers une image du catalogue {@code media}, par
 * identifiant (ADR 0002), légende facultative (300 caractères au plus) et ordre d'affichage (positif ou nul).
 */
public record ProjectScreenshot(Long mediaId, String caption, int displayOrder) {

    public static final int MAX_CAPTION_LENGTH = 300;

    /**
     * Ordre d'affichage, puis identifiant du média : ordre total.
     */
    static final Comparator<ProjectScreenshot> DISPLAY_ORDER = Comparator
        .comparingInt(ProjectScreenshot::displayOrder)
        .thenComparing(ProjectScreenshot::mediaId);

    public ProjectScreenshot {
        Objects.requireNonNull(mediaId, "mediaId");
        if (displayOrder < 0) {
            throw new IllegalArgumentException("display order must not be negative");
        }
        if (caption != null && caption.length() > MAX_CAPTION_LENGTH) {
            throw new IllegalArgumentException("caption must be at most 300 characters");
        }
    }
}
