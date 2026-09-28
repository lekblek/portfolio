package com.scalke.portfolio.backend.media.domain.model;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Formats de média acceptés en V1 (D08) : images PNG, JPEG, WebP et documents PDF. Aucun SVG, qui peut
 * embarquer du script. Le format d'un fichier stocké se lit dans l'extension de sa clé (D-BP).
 * <p>
 * Un fichier envoyé est reconnu par sa signature (ses premiers octets), jamais par son nom ni par le type
 * annoncé par le client ; sa taille ne dépasse pas celle de son format : 5 Mio pour une image, 10 Mio pour
 * un PDF ({@code 01} §12, D-BR).
 */
public enum MediaFormat {

    PNG("image/png", "png", 5 * Size.MIB,
        content -> startsWith(content, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)),
    JPEG("image/jpeg", "jpg", 5 * Size.MIB,
        content -> startsWith(content, 0, 0xFF, 0xD8, 0xFF)),
    WEBP("image/webp", "webp", 5 * Size.MIB,
        content -> startsWith(content, 0, 'R', 'I', 'F', 'F') && startsWith(content, 8, 'W', 'E', 'B', 'P')),
    PDF("application/pdf", "pdf", 10 * Size.MIB,
        content -> startsWith(content, 0, '%', 'P', 'D', 'F', '-'));

    private final String mimeType;
    private final String extension;
    private final long maxSize;
    private final Predicate<byte[]> signature;

    MediaFormat(String mimeType, String extension, long maxSize, Predicate<byte[]> signature) {
        this.mimeType = mimeType;
        this.extension = extension;
        this.maxSize = maxSize;
        this.signature = signature;
    }

    public String mimeType() {
        return mimeType;
    }

    public String extension() {
        return extension;
    }

    /**
     * Taille maximale d'un fichier de ce format, en octets.
     */
    public long maxSize() {
        return maxSize;
    }

    /**
     * Format reconnu d'après la signature du contenu ; vide si aucun format accepté ne correspond (fichier
     * vide compris).
     */
    public static Optional<MediaFormat> detect(byte[] content) {
        return Arrays.stream(values()).filter(format -> format.signature.test(content)).findFirst();
    }

    /**
     * Plus grande taille acceptée, tous formats confondus : borne de lecture d'un fichier envoyé.
     */
    public static long maxAcceptedSize() {
        return Arrays.stream(values()).mapToLong(MediaFormat::maxSize).max().orElseThrow();
    }

    static Optional<MediaFormat> fromExtension(String extension) {
        return Arrays.stream(values()).filter(format -> format.extension.equals(extension)).findFirst();
    }

    private static boolean startsWith(byte[] content, int offset, int... expected) {
        if (content.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((content[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Unité des limites : le mébioctet (1 048 576 octets), que les systèmes de fichiers affichent « Mo ».
     */
    private static final class Size {

        private static final long MIB = 1024 * 1024;
    }
}
