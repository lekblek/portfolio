package com.scalke.portfolio.backend.media.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaTest {

    private static final StorageKey IMAGE = new StorageKey("3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.webp");
    private static final StorageKey DOCUMENT = new StorageKey("3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.pdf");
    private static final Instant AT = Instant.parse("2026-06-15T10:00:00Z");

    @Test
    void derives_its_type_from_its_key() {
        Media media = new Media(null, IMAGE, "couverture.webp", 1_000, new Dimensions(640, 480), "Une couverture", AT);

        assertThat(media.format()).isEqualTo(MediaFormat.WEBP);
        assertThat(media.mimeType()).isEqualTo("image/webp");
    }

    @Test
    void an_image_has_dimensions_and_a_pdf_has_none() {
        assertThatThrownBy(() -> new Media(null, IMAGE, "couverture.webp", 1_000, null, null, AT))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Media(null, DOCUMENT, "cv.pdf", 1_000, new Dimensions(1, 1), null, AT))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(new Media(null, DOCUMENT, "cv.pdf", 1_000, null, null, AT).dimensions()).isNull();
    }

    @Test
    void respects_the_size_limit_of_its_format() {
        assertThatThrownBy(() -> new Media(null, IMAGE, "a.webp", 5 * 1024 * 1024 + 1, new Dimensions(1, 1), null, AT))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Media(null, DOCUMENT, "cv.pdf", 0, null, null, AT))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requires_a_name_and_limits_the_alt_text() {
        assertThatThrownBy(() -> new Media(null, DOCUMENT, " ", 1, null, null, AT))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Media(null, DOCUMENT, "a".repeat(256), 1, null, null, AT))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Media(null, DOCUMENT, "cv.pdf", 1, null, "a".repeat(301), AT))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_non_positive_dimensions() {
        assertThatThrownBy(() -> new Dimensions(0, 10)).isInstanceOf(IllegalArgumentException.class);
    }
}
