package com.scalke.portfolio.backend.publication;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contraintes de {@code V006__create_publication.sql} (et {@code V009} pour la date des archives), vérifiées sans JPA.
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
        assertThatThrownBy(() -> insert("article", "ARTICLE", status, null))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("publication_published_at_check");
    }

    private void insert(String slug, String type, String status, OffsetDateTime publishedAt) {
        jdbcClient.sql("""
                    INSERT INTO publication (type, title, slug, summary, content_markdown, status,
                                             published_at, created_at, updated_at)
                    VALUES (:type, 'Titre', :slug, 'Résumé', '# Contenu', :status, :publishedAt, :at, :at)
                    """)
            .param("type", type)
            .param("slug", slug)
            .param("status", status)
            .param("publishedAt", publishedAt)
            .param("at", AT)
            .update();
    }
}
