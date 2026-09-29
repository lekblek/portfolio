package com.scalke.portfolio.backend.project;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contraintes de {@code V013__add_media_to_project.sql}, vérifiées sans JPA : invariants 13 et 27 (D-BV).
 */
@Transactional
class ProjectMediaSchemaIT extends AbstractIntegrationTest {

    @Autowired
    JdbcClient jdbcClient;

    private long project;
    private long image;

    @BeforeEach
    void givenAProjectAndAnImage() {
        project = jdbcClient.sql("""
                    INSERT INTO project (title, slug, short_description, description_markdown, stage, visibility,
                                         start_date, featured, display_order)
                    VALUES ('Projet', 'projet', 'Résumé', '# Projet', 'IN_PROGRESS', 'DRAFT', DATE '2026-01-01',
                            FALSE, 0)
                    RETURNING id
                    """)
            .query(Long.class)
            .single();
        image = insertMedia("3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.png");
    }

    @Test
    void refuses_to_delete_a_cover() {
        jdbcClient.sql("UPDATE project SET cover_media_id = :image WHERE id = :project")
            .param("image", image)
            .param("project", project)
            .update();

        assertThatThrownBy(this::deleteImage)
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("project_cover_media_fk");
    }

    @Test
    void refuses_to_delete_a_screenshot() {
        insertScreenshot(image, 0);

        assertThatThrownBy(this::deleteImage)
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("project_screenshot_media_fk");
    }

    @Test
    void rejects_the_same_screenshot_twice() {
        insertScreenshot(image, 0);

        assertThatThrownBy(() -> insertScreenshot(image, 1))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("project_screenshot_pk");
    }

    @Test
    void rejects_a_negative_display_order() {
        assertThatThrownBy(() -> insertScreenshot(image, -1))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("project_screenshot_display_order_check");
    }

    @Test
    void deleting_a_project_deletes_its_screenshots_but_keeps_the_media() {
        insertScreenshot(image, 0);

        jdbcClient.sql("DELETE FROM project WHERE id = :project").param("project", project).update();

        assertThat(count("project_screenshot")).isZero();
        assertThat(count("media")).isEqualTo(1);
    }

    private long insertMedia(String key) {
        return jdbcClient.sql("""
                    INSERT INTO media (storage_key, original_name, size_bytes, width, height, created_at)
                    VALUES (:key, 'image.png', 10, 1, 1, now())
                    RETURNING id
                    """)
            .param("key", key)
            .query(Long.class)
            .single();
    }

    private void insertScreenshot(long media, int displayOrder) {
        jdbcClient.sql("""
                    INSERT INTO project_screenshot (project_id, media_id, display_order)
                    VALUES (:project, :media, :order)
                    """)
            .param("project", project)
            .param("media", media)
            .param("order", displayOrder)
            .update();
    }

    private void deleteImage() {
        jdbcClient.sql("DELETE FROM media WHERE id = :image").param("image", image).update();
    }

    private long count(String table) {
        return jdbcClient.sql("SELECT count(*) FROM " + table).query(Long.class).single();
    }
}
