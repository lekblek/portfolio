package com.scalke.portfolio.backend.media.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.scalke.portfolio.backend.media.MediaSamples.JPEG;
import static com.scalke.portfolio.backend.media.MediaSamples.PDF;
import static com.scalke.portfolio.backend.media.MediaSamples.PNG;
import static com.scalke.portfolio.backend.media.MediaSamples.WEBP;
import static org.assertj.core.api.Assertions.assertThat;

class ImageDimensionsTest {

    @Test
    void reads_the_dimensions_of_images_produced_by_a_real_encoder() {
        assertThat(ImageDimensions.read(MediaFormat.PNG, PNG)).contains(new Dimensions(3, 2));
        assertThat(ImageDimensions.read(MediaFormat.JPEG, JPEG)).contains(new Dimensions(4, 5));
    }

    @Test
    void reads_the_three_kinds_of_webp_header() {
        assertThat(ImageDimensions.read(MediaFormat.WEBP, WEBP)).contains(new Dimensions(640, 480));
        assertThat(ImageDimensions.read(MediaFormat.WEBP, webp("VP8L",
            0x2F, 0x3F, 0x00, 0x00, 0x00)))
            .as("sans perte : largeur 64, hauteur 1").contains(new Dimensions(64, 1));
        assertThat(ImageDimensions.read(MediaFormat.WEBP, webp("VP8 ",
            0x00, 0x00, 0x00, 0x9D, 0x01, 0x2A, 0x20, 0x03, 0x58, 0x02)))
            .as("avec perte : 800 × 600").contains(new Dimensions(800, 600));
    }

    @Test
    void reads_a_progressive_jpeg() {
        byte[] progressive = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xC2, 0x00, 0x11, 0x08, 0x00, 0x10,
            0x00, 0x20, 0x03, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        assertThat(ImageDimensions.read(MediaFormat.JPEG, progressive)).contains(new Dimensions(32, 16));
    }

    /**
     * La signature seule ne suffit pas : en-tête tronqué, absent ou incohérent → illisible.
     */
    @Test
    void finds_nothing_in_a_broken_header() {
        assertThat(ImageDimensions.read(MediaFormat.PNG, Arrays.copyOf(PNG, 20))).isEmpty();
        assertThat(ImageDimensions.read(MediaFormat.PNG, Arrays.copyOf(PNG, 8))).isEmpty();
        assertThat(ImageDimensions.read(MediaFormat.JPEG, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})).isEmpty();
        assertThat(ImageDimensions.read(MediaFormat.JPEG,
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xDA, 0x00, 0x08, 0, 0, 0, 0, 0, 0}))
            .as("début de l'image avant tout SOF").isEmpty();
        assertThat(ImageDimensions.read(MediaFormat.WEBP, Arrays.copyOf(WEBP, 12))).isEmpty();
        assertThat(ImageDimensions.read(MediaFormat.WEBP, webp("VP8 ", 0, 0, 0, 0, 0, 0, 0x20, 0x03, 0x58, 0x02)))
            .as("code de départ absent").isEmpty();
    }

    @Test
    void a_pdf_has_no_dimensions() {
        assertThat(ImageDimensions.read(MediaFormat.PDF, PDF)).isEmpty();
    }

    private static byte[] webp(String chunk, int... data) {
        byte[] content = new byte[32];
        System.arraycopy(new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}, 0, content, 0, 12);
        for (int i = 0; i < 4; i++) {
            content[12 + i] = (byte) chunk.charAt(i);
        }
        for (int i = 0; i < data.length; i++) {
            content[20 + i] = (byte) data[i];
        }
        return content;
    }
}
