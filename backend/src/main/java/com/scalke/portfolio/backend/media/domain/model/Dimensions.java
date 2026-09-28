package com.scalke.portfolio.backend.media.domain.model;

/**
 * Largeur et hauteur d'une image, en pixels.
 */
public record Dimensions(int width, int height) {

    public Dimensions {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("dimensions must be positive, got " + width + "x" + height);
        }
    }
}
