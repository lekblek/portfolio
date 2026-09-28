package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.media.application.query.PublicImage;

/**
 * Capture d'un projet publié, sous sa forme publique : l'image et sa légende ({@code null} si aucune).
 */
public record PublishedScreenshot(PublicImage image, String caption) {
}
