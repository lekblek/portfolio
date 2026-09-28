package com.scalke.portfolio.backend.media;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contraintes de {@code V012__create_media.sql}, vérifiées sans JPA (D-BU).
 */
@Transactional
class MediaSchemaIT extends AbstractIntegrationTest {

    private static final String HEX = "3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a";
    private static final long MIB = 1024 * 1024;

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void accepts_an_image_with_dimensions_and_a_pdf_without() {
        insert(HEX + ".webp", 5 * MIB, 640, 480);
        insert("0123456789abcdef0123456789abcdef.pdf", 10 * MIB, null, null);

        assertThat(jdbcClient.sql("SELECT count(*) FROM media").query(Long.class).single()).isEqualTo(2);
    }

    @Test
    void rejects_a_duplicated_key() {
        insert(HEX + ".png", 1, 1, 1);

        assertThatThrownBy(() -> insert(HEX + ".png", 1, 1, 1))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("media_storage_key_unique");
    }

    @ParameterizedTest
    @CsvSource({
        "../3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.png",
        "3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.svg",
        "avatar.png",
    })
    void rejects_a_key_that_is_not_opaque(String key) {
        assertThatThrownBy(() -> insert(key, 1, 1, 1))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("media_storage_key_format_check");
    }

    @ParameterizedTest
    @CsvSource({"png, 5242881", "pdf, 10485761", "pdf, 0"})
    void rejects_a_size_outside_the_limit_of_the_format(String extension, long size) {
        Integer dimension = extension.equals("pdf") ? null : 1;

        assertThatThrownBy(() -> insert(HEX + "." + extension, size, dimension, dimension))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("media_size_check");
    }

    /**
     * Un CHECK dont l'expression vaut NULL est satisfait : la contrainte teste la présence explicitement.
     */
    @ParameterizedTest
    @CsvSource(value = {"png, NULL, NULL", "png, 10, NULL", "png, 0, 10", "pdf, 10, 10"}, nullValues = "NULL")
    void requires_dimensions_for_an_image_and_none_for_a_pdf(String extension, Integer width, Integer height) {
        assertThatThrownBy(() -> insert(HEX + "." + extension, 1, width, height))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("media_dimensions_check");
    }

    @Test
    void requires_an_original_name() {
        assertThatThrownBy(() -> jdbcClient.sql("""
                    INSERT INTO media (storage_key, original_name, size_bytes, created_at)
                    VALUES (:key, '  ', 1, now())
                    """)
            .param("key", HEX + ".pdf")
            .update())
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("media_original_name_check");
    }

    private void insert(String key, long size, Integer width, Integer height) {
        jdbcClient.sql("""
                    INSERT INTO media (storage_key, original_name, size_bytes, width, height, created_at)
                    VALUES (:key, 'fichier', :size, :width, :height, :at)
                    """)
            .param("key", key)
            .param("size", size)
            .param("width", width)
            .param("height", height)
            .param("at", OffsetDateTime.parse("2026-06-15T10:00:00Z"))
            .update();
    }
}
