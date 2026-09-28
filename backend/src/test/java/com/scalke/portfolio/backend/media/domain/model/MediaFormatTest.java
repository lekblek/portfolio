package com.scalke.portfolio.backend.media.domain.model;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static com.scalke.portfolio.backend.media.MediaSamples.JPEG;
import static com.scalke.portfolio.backend.media.MediaSamples.PDF;
import static com.scalke.portfolio.backend.media.MediaSamples.PNG;
import static com.scalke.portfolio.backend.media.MediaSamples.WEBP;
import static org.assertj.core.api.Assertions.assertThat;

class MediaFormatTest {

    @Test
    void recognises_each_accepted_format_by_its_signature() {
        assertThat(MediaFormat.detect(PNG)).contains(MediaFormat.PNG);
        assertThat(MediaFormat.detect(JPEG)).contains(MediaFormat.JPEG);
        assertThat(MediaFormat.detect(WEBP)).contains(MediaFormat.WEBP);
        assertThat(MediaFormat.detect(PDF)).contains(MediaFormat.PDF);
    }

    /**
     * D-BR : ni SVG, ni GIF, ni HTML, ni un autre conteneur RIFF (WAV) ; un en-tête tronqué ne suffit pas.
     */
    @Test
    void recognises_nothing_else() {
        assertThat(MediaFormat.detect(ascii("<svg xmlns=\"http://www.w3.org/2000/svg\">"))).isEmpty();
        assertThat(MediaFormat.detect(ascii("GIF89a"))).isEmpty();
        assertThat(MediaFormat.detect(ascii("<!DOCTYPE html>"))).isEmpty();
        assertThat(MediaFormat.detect(ascii("RIFF\0\0\0\0WAVE"))).isEmpty();
        assertThat(MediaFormat.detect(ascii("RIFF"))).isEmpty();
        assertThat(MediaFormat.detect(new byte[0])).isEmpty();
    }

    /**
     * {@code 01} §12 : 5 Mio pour une image, 10 Mio pour un PDF.
     */
    @Test
    void limits_the_size_of_each_format() {
        assertThat(MediaFormat.PNG.maxSize()).isEqualTo(5 * 1024 * 1024);
        assertThat(MediaFormat.JPEG.maxSize()).isEqualTo(5 * 1024 * 1024);
        assertThat(MediaFormat.WEBP.maxSize()).isEqualTo(5 * 1024 * 1024);
        assertThat(MediaFormat.PDF.maxSize()).isEqualTo(10 * 1024 * 1024);
        assertThat(MediaFormat.maxAcceptedSize()).isEqualTo(10 * 1024 * 1024);
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }
}
