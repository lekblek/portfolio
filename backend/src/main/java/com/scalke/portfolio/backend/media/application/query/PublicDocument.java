package com.scalke.portfolio.backend.media.application.query;

import com.scalke.portfolio.backend.media.domain.model.Media;

/**
 * Document (PDF) tel que le propose le site public (D-BX) : adresse du fichier ({@code GET /api/public/media/...})
 * et taille en octets, pour l'annoncer avant le téléchargement.
 */
public record PublicDocument(String url, long sizeBytes) {

    static PublicDocument of(Media document) {
        return new PublicDocument(MediaQueryService.PUBLIC_PATH + document.storageKey().value(), document.size());
    }
}
