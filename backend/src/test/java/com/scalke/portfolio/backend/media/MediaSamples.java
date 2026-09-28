package com.scalke.portfolio.backend.media;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

/**
 * Contenus de test reconnus par leur signature (D-BR) et dont les dimensions se lisent (D-BU) : PNG et JPEG
 * produits par l'encodeur de la JDK, WebP écrits à la main (la JDK n'encode pas le WebP), PDF minimal.
 */
public final class MediaSamples {

    /**
     * PNG réel de 3 × 2 pixels.
     */
    public static final byte[] PNG = encode("png", 3, 2);

    /**
     * JPEG réel de 4 × 5 pixels.
     */
    public static final byte[] JPEG = encode("jpg", 4, 5);

    /**
     * WebP étendu (VP8X) de 640 × 480 pixels : canevas moins 1, sur 24 bits petit-boutiste.
     */
    public static final byte[] WEBP = {
        'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P',
        'V', 'P', '8', 'X', 10, 0, 0, 0,
        0, 0, 0, 0,
        (byte) 0x7F, 0x02, 0x00,
        (byte) 0xDF, 0x01, 0x00,
        0, 0};

    public static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};

    private MediaSamples() {
    }

    public static byte[] ofSize(byte[] sample, int size) {
        return Arrays.copyOf(sample, size);
    }

    private static byte[] encode(String format, int width, int height) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
