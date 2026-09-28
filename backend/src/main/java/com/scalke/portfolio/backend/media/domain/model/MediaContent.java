package com.scalke.portfolio.backend.media.domain.model;

import java.io.InputStream;
import java.util.Objects;

/**
 * Contenu d'un fichier stocké : sa taille en octets et un flux de lecture, que l'appelant doit fermer.
 * Un flux plutôt qu'un chemin : le domaine ignore où et comment le fichier est stocké (D-BO).
 */
public record MediaContent(long size, InputStream stream) {

    public MediaContent {
        Objects.requireNonNull(stream, "stream");
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative");
        }
    }
}
