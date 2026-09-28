package com.scalke.portfolio.backend.media.domain.model;

import java.util.Optional;

/**
 * Lecture des dimensions d'une image dans son en-tête, sans la décoder (D-BU) : IHDR pour PNG, segment
 * SOF pour JPEG, en-tête VP8X, VP8L ou VP8 pour WebP. Vide si l'en-tête est absent, tronqué ou incohérent :
 * la signature seule ne suffit pas à accepter une image.
 */
public final class ImageDimensions {

    private ImageDimensions() {
    }

    public static Optional<Dimensions> read(MediaFormat format, byte[] content) {
        return switch (format) {
            case PNG -> png(content);
            case JPEG -> jpeg(content);
            case WEBP -> webp(content);
            case PDF -> Optional.empty();
        };
    }

    /**
     * Premier bloc après la signature : longueur (4 octets), {@code IHDR}, largeur et hauteur (4 octets
     * chacune, gros-boutiste).
     */
    private static Optional<Dimensions> png(byte[] content) {
        if (content.length < 24 || !ascii(content, 12, "IHDR")) {
            return Optional.empty();
        }
        return dimensions(int32(content, 16), int32(content, 20));
    }

    /**
     * Parcours des segments jusqu'au premier SOF (C0 à CF, sauf C4, C8 et CC) : hauteur puis largeur sur
     * 2 octets, après la longueur et la précision. Un début d'image ({@code SOS}) avant tout SOF est invalide.
     */
    private static Optional<Dimensions> jpeg(byte[] content) {
        int position = 2;
        while (position + 3 < content.length) {
            if ((content[position] & 0xFF) != 0xFF) {
                return Optional.empty();
            }
            int marker = content[position + 1] & 0xFF;
            if (marker == 0xFF) {
                position++;
                continue;
            }
            if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD8)) {
                position += 2;
                continue;
            }
            if (marker == 0xD9 || marker == 0xDA) {
                return Optional.empty();
            }
            int length = uint16(content, position + 2);
            boolean startOfFrame = marker >= 0xC0 && marker <= 0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC;
            if (startOfFrame) {
                if (position + 8 >= content.length) {
                    return Optional.empty();
                }
                return dimensions(uint16(content, position + 7), uint16(content, position + 5));
            }
            if (length < 2) {
                return Optional.empty();
            }
            position += 2 + length;
        }
        return Optional.empty();
    }

    /**
     * Premier bloc du conteneur RIFF (octet 12) : {@code VP8X} (dimensions du canevas moins 1, 24 bits
     * petit-boutiste), {@code VP8L} (sans perte : 14 bits chacune, moins 1) ou {@code VP8 } (avec perte :
     * 14 bits chacune après le code de départ {@code 9D 01 2A}).
     */
    private static Optional<Dimensions> webp(byte[] content) {
        if (content.length < 30) {
            return Optional.empty();
        }
        if (ascii(content, 12, "VP8X")) {
            return dimensions(1 + uint24le(content, 24), 1 + uint24le(content, 27));
        }
        if (ascii(content, 12, "VP8L")) {
            if ((content[20] & 0xFF) != 0x2F) {
                return Optional.empty();
            }
            int b0 = content[21] & 0xFF;
            int b1 = content[22] & 0xFF;
            int b2 = content[23] & 0xFF;
            int b3 = content[24] & 0xFF;
            int width = 1 + (((b1 & 0x3F) << 8) | b0);
            int height = 1 + (((b3 & 0x0F) << 10) | (b2 << 2) | ((b1 & 0xC0) >> 6));
            return dimensions(width, height);
        }
        if (ascii(content, 12, "VP8 ")) {
            if ((content[23] & 0xFF) != 0x9D || (content[24] & 0xFF) != 0x01 || (content[25] & 0xFF) != 0x2A) {
                return Optional.empty();
            }
            return dimensions(uint16le(content, 26) & 0x3FFF, uint16le(content, 28) & 0x3FFF);
        }
        return Optional.empty();
    }

    private static Optional<Dimensions> dimensions(long width, long height) {
        if (width < 1 || height < 1 || width > Integer.MAX_VALUE || height > Integer.MAX_VALUE) {
            return Optional.empty();
        }
        return Optional.of(new Dimensions((int) width, (int) height));
    }

    private static boolean ascii(byte[] content, int offset, String expected) {
        if (content.length < offset + expected.length()) {
            return false;
        }
        for (int i = 0; i < expected.length(); i++) {
            if (content[offset + i] != expected.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static long int32(byte[] content, int offset) {
        return ((long) (content[offset] & 0xFF) << 24) | ((content[offset + 1] & 0xFF) << 16)
            | ((content[offset + 2] & 0xFF) << 8) | (content[offset + 3] & 0xFF);
    }

    private static int uint16(byte[] content, int offset) {
        return ((content[offset] & 0xFF) << 8) | (content[offset + 1] & 0xFF);
    }

    private static int uint16le(byte[] content, int offset) {
        return (content[offset] & 0xFF) | ((content[offset + 1] & 0xFF) << 8);
    }

    private static int uint24le(byte[] content, int offset) {
        return (content[offset] & 0xFF) | ((content[offset + 1] & 0xFF) << 8) | ((content[offset + 2] & 0xFF) << 16);
    }
}
