package com.scalke.portfolio.backend.media.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageKeyTest {

    private static final String HEX = "3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a";

    @ParameterizedTest
    @CsvSource({
        "png,  PNG,  image/png",
        "jpg,  JPEG, image/jpeg",
        "webp, WEBP, image/webp",
        "pdf,  PDF,  application/pdf",
    })
    void reads_the_format_from_the_extension(String extension, MediaFormat format, String mimeType) {
        StorageKey key = new StorageKey(HEX + "." + extension);

        assertThat(key.format()).isEqualTo(format);
        assertThat(key.format().mimeType()).isEqualTo(mimeType);
    }

    /**
     * D-BP : ni chemin, ni navigation, ni format hors D08, ni variante de casse.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "../3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.png",
        "sub/3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.png",
        "sub\\3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.png",
        "3F2A9C0E8D7B4A1F9E6C5B4A3D2E1F0A.png",
        "3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.PNG",
        "3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.svg",
        "3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.jpeg",
        "3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a",
        "3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0.png",
        "3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.png.pdf",
        "avatar.png",
        ""
    })
    void rejects_anything_that_is_not_an_opaque_key(String value) {
        assertThat(StorageKey.parse(value)).isEmpty();
        assertThatThrownBy(() -> new StorageKey(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parses_a_valid_key() {
        assertThat(StorageKey.parse(HEX + ".webp")).contains(new StorageKey(HEX + ".webp"));
        assertThat(StorageKey.parse(null)).isEmpty();
    }
}
