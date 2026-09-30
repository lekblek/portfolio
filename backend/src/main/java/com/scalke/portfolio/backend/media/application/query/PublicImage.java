package com.scalke.portfolio.backend.media.application.query;

import com.scalke.portfolio.backend.media.domain.model.Media;
import org.jspecify.annotations.Nullable;

/**
 * Image telle que l'affiche le site public (D-BW) : adresse du fichier ({@code GET /api/public/media/...}),
 * dimensions en pixels (pour réserver la place avant le chargement), texte alternatif ({@code null} si
 * aucun). Contrat JSON commun à tous les modules qui affichent une image.
 */
public record PublicImage(String url, int width, int height, @Nullable String altText) {

    static PublicImage of(Media image) {
        return new PublicImage(
            MediaQueryService.PUBLIC_PATH + image.storageKey().value(),
            image.dimensions().width(),
            image.dimensions().height(),
            image.altText());
    }
}
