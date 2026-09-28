package com.scalke.portfolio.backend.media;

import java.util.Arrays;

/**
 * Contenus minimaux reconnus par leur signature (D-BR), complétés par des zéros jusqu'à la taille voulue.
 */
public final class MediaSamples {

    public static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    public static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    public static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
    public static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};

    private MediaSamples() {
    }

    public static byte[] ofSize(byte[] signature, int size) {
        return Arrays.copyOf(signature, size);
    }
}
