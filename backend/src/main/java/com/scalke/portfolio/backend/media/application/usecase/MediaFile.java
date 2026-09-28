package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.domain.model.MediaContent;
import com.scalke.portfolio.backend.media.domain.model.MediaFormat;

/**
 * Fichier d'un média prêt à être servi : son format (type MIME) et son contenu, dont le flux est fermé par
 * l'appelant.
 */
public record MediaFile(MediaFormat format, MediaContent content) {
}
