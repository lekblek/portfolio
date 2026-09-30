package com.scalke.portfolio.backend.media.web.dto;

import com.scalke.portfolio.backend.media.application.query.MediaQueryService;
import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.model.MediaFormat;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Média vu par l'administration (D-CT) : identifiant (pour le référencer depuis un contenu), adresse publique du
 * fichier, nom d'origine, format et type MIME, taille, dimensions ({@code null} pour un PDF), texte alternatif.
 */
public record AdminMediaResponse(
    Long id,
    String url,
    String originalName,
    MediaFormat format,
    String mimeType,
    long sizeBytes,
    @Nullable Integer width,
    @Nullable Integer height,
    @Nullable String altText,
    Instant createdAt
) {

    public static AdminMediaResponse from(Media media) {
        boolean hasDimensions = media.dimensions() != null;
        return new AdminMediaResponse(
            media.id(),
            MediaQueryService.publicUrl(media.storageKey()),
            media.originalName(),
            media.format(),
            media.mimeType(),
            media.size(),
            hasDimensions ? media.dimensions().width() : null,
            hasDimensions ? media.dimensions().height() : null,
            media.altText(),
            media.createdAt());
    }
}
