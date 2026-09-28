package com.scalke.portfolio.backend.media.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Média du catalogue (racine d'agrégat, {@code 02} §21, D-BU) : métadonnées d'un fichier stocké sous
 * {@code storageKey}. Le type MIME se déduit du format de la clé ; il n'est pas stocké.
 * <p>
 * Invariants, doublés par PostgreSQL : nom d'origine non vide (255 caractères au plus), taille strictement
 * positive et dans la limite du format (D-BR), dimensions présentes pour une image et absentes pour un PDF,
 * texte alternatif de 300 caractères au plus.
 */
public record Media(
    Long id,
    StorageKey storageKey,
    String originalName,
    long size,
    Dimensions dimensions,
    String altText,
    Instant createdAt
) {

    public static final int MAX_ORIGINAL_NAME_LENGTH = 255;
    public static final int MAX_ALT_TEXT_LENGTH = 300;

    public Media {
        Objects.requireNonNull(storageKey, "storageKey");
        Objects.requireNonNull(originalName, "originalName");
        Objects.requireNonNull(createdAt, "createdAt");
        if (originalName.isBlank() || originalName.length() > MAX_ORIGINAL_NAME_LENGTH) {
            throw new IllegalArgumentException("original name must be 1 to 255 characters");
        }
        MediaFormat format = storageKey.format();
        if (size < 1 || size > format.maxSize()) {
            throw new IllegalArgumentException("size " + size + " outside 1.." + format.maxSize() + " for " + format);
        }
        if (format.isImage() != (dimensions != null)) {
            throw new IllegalArgumentException("an image has dimensions, a PDF has none");
        }
        if (altText != null && altText.length() > MAX_ALT_TEXT_LENGTH) {
            throw new IllegalArgumentException("alt text must be at most 300 characters");
        }
    }

    public MediaFormat format() {
        return storageKey.format();
    }

    public String mimeType() {
        return format().mimeType();
    }
}
