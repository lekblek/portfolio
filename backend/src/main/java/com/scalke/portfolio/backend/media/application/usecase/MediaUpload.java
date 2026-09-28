package com.scalke.portfolio.backend.media.application.usecase;

import java.io.InputStream;
import java.util.Objects;

/**
 * Fichier envoyé : nom d'origine annoncé par le client, texte alternatif facultatif, contenu (lu, jamais
 * fermé, par le cas d'usage).
 */
public record MediaUpload(String originalName, String altText, InputStream content) {

    public MediaUpload {
        Objects.requireNonNull(content, "content");
    }
}
