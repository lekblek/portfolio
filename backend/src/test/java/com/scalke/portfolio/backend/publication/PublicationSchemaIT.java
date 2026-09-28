package com.scalke.portfolio.backend.publication;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contraintes de {@code V006__create_publication.sql}, {@code V009} (date des archives) et {@code V010}
 * (première publication, D-AZ), vérifiées sans JPA.
 */
@Transactional
class PublicationSchemaIT extends AbstractIntegrationTest {

    private static final OffsetDateTime AT = OffsetDateTime.parse("2026-06-01T09:00:00Z");

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void accepts_a_draft_without_publication_date() {
        insert("brouillon", "ARTICLE", "DRAFT", null);

        assertThat(jdbcClient.sql("SELECT count(*) FROM publication").query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void shares_one_slug_space_between_articles_and_news() {
        insert("annonce", "ARTICLE", "PUBLISHED", AT);

        assertThatThrownBy(() -> insert("annonce", "NEWS", "PUBLISHED", AT))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("publication_slug_unique");
    }

    @Test
    void rejects_a_slug_that_is_not_url_friendly() {
        assertThatThrownBy(() -> insert("Mon Article", "ARTICLE", "DRAFT", null))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("publication_slug_format_check");
    }

    @Test
    void rejects_an_unknown_type() {
        assertThatThrownBy(() -> insert("tutoriel", "TUTORIAL", "DRAFT", null))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("publication_type_check");
    }

    @Test
    void rejects_an_unknown_status() {
        assertThatThrownBy(() -> insert("article", "ARTICLE", "DELETED", null))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("publication_status_check");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SCHEDULED", "PUBLISHED", "ARCHIVED"})
    void requires_a_publication_date_once_scheduled_published_or_archived(String status) {
        assertThatThrownBy(() -> insert("article", "ARTICLE", status, null, AT))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("publication_published_at_check");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SCHEDULED", "PUBLISHED", "ARCHIVED"})
    void requires_a_first_publication_date_once_scheduled_published_or_archived(String status) {
        assertThatThrownBy(() -> insert("article", "ARTICLE", status, AT, null))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("publication_first_published_at_check");
    }

    /**
     * Une publication repassée en brouillon garde la mémoire de sa première publication (D-AZ, audit A01).
     */
    @Test
    void accepts_a_draft_that_has_already_been_public() {
        insert("reprise", "ARTICLE", "DRAFT", null, AT);

        assertThat(jdbcClient.sql("SELECT first_published_at FROM publication").query(OffsetDateTime.class).single())
            .isEqualTo(AT);
    }

    @Test
    void rejects_a_first_publication_after_the_publication_date() {
        assertThatThrownBy(() -> insert("article", "ARTICLE", "PUBLISHED", AT, AT.plus(Duration.ofDays(1))))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("publication_first_published_before_published_check");
    }

    /**
     * Invariant 13 (D-BY) : une couverture utilisée ne peut pas être supprimée.
     */
    @Test
    void refuses_to_delete_a_cover() {
        long media = jdbcClient.sql("""
                    INSERT INTO media (storage_key, original_name, size_bytes, width, height, created_at)
                    VALUES ('3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.png', 'couverture.png', 10, 1, 1, now())
                    RETURNING id
                    """)
            .query(Long.class)
            .single();
        insert("article", "ARTICLE", "DRAFT", null);
        jdbcClient.sql("UPDATE publication SET cover_media_id = :media").param("media", media).update();

        assertThatThrownBy(() -> jdbcClient.sql("DELETE FROM media WHERE id = :media").param("media", media).update())
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("publication_cover_media_fk");
    }

    private void insert(String slug, String type, String status, OffsetDateTime publishedAt) {
        insert(slug, type, status, publishedAt, publishedAt);
    }

    private void insert(String slug, String type, String status, OffsetDateTime publishedAt,
                        OffsetDateTime firstPublishedAt) {
        jdbcClient.sql("""
                    INSERT INTO publication (type, title, slug, summary, content_markdown, status,
                                             published_at, first_published_at, created_at, updated_at)
                    VALUES (:type, 'Titre', :slug, 'Résumé', '# Contenu', :status, :publishedAt,
                            :firstPublishedAt, :at, :at)
                    """)
            .param("type", type)
            .param("slug", slug)
            .param("status", status)
            .param("publishedAt", publishedAt)
            .param("firstPublishedAt", firstPublishedAt)
            .param("at", AT)
            .update();
    }
}
