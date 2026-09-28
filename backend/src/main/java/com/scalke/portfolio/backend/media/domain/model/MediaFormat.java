package com.scalke.portfolio.backend.media.domain.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formats de média acceptés en V1 (D08) : images PNG, JPEG, WebP et documents PDF. Aucun SVG, qui peut
 * embarquer du script. Le format d'un fichier stocké se lit dans l'extension de sa clé (D-BP).
 */
public enum MediaFormat {

    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg"),
    WEBP("image/webp", "webp"),
    PDF("application/pdf", "pdf");

    private final String mimeType;
    private final String extension;

    MediaFormat(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String mimeType() {
        return mimeType;
    }

    public String extension() {
        return extension;
    }

    static Optional<MediaFormat> fromExtension(String extension) {
        return Arrays.stream(values()).filter(format -> format.extension.equals(extension)).findFirst();
    }
}
