package com.scalke.portfolio.backend.publication;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contraintes de {@code V008__add_taxonomy_to_publication.sql}, vérifiées sans JPA.
 */
@Transactional
class PublicationTaxonomySchemaIT extends AbstractIntegrationTest {

    private static final OffsetDateTime AT = OffsetDateTime.parse("2026-06-01T09:00:00Z");

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void rejects_an_unknown_category() {
        assertThatThrownBy(() -> insertPublication("article", 999_999L))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("publication_category_fk");
    }

    @Test
    void refuses_to_delete_a_category_used_by_a_publication() {
        long backend = insertTerm("category", "Backend", "backend");
        insertPublication("article", backend);

        assertThatThrownBy(() -> delete("category", backend))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("publication_category_fk");
    }

    @Test
    void rejects_the_same_tag_twice_on_a_publication() {
        long publication = insertPublication("article", null);
        long java = insertTerm("tag", "Java", "java");
        tag(publication, java);

        assertThatThrownBy(() -> tag(publication, java))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("publication_tag_pk");
    }

    @Test
    void refuses_to_delete_a_tag_used_by_a_publication() {
        long java = insertTerm("tag", "Java", "java");
        tag(insertPublication("article", null), java);

        assertThatThrownBy(() -> delete("tag", java))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("publication_tag_tag_fk");
    }

    @Test
    void deleting_a_publication_removes_its_tags_but_keeps_the_terms() {
        long publication = insertPublication("article", insertTerm("category", "Backend", "backend"));
        tag(publication, insertTerm("tag", "Java", "java"));

        delete("publication", publication);

        assertThat(count("publication_tag")).isZero();
        assertThat(count("tag")).isEqualTo(1);
        assertThat(count("category")).isEqualTo(1);
    }

    private long insertPublication(String slug, Long categoryId) {
        return jdbcClient.sql("""
                    INSERT INTO publication (type, title, slug, summary, content_markdown, status,
                                             category_id, created_at, updated_at)
                    VALUES ('ARTICLE', 'Titre', :slug, 'Résumé', '# Contenu', 'DRAFT', :category, :at, :at)
                    RETURNING id
                    """)
            .param("slug", slug)
            .param("category", categoryId)
            .param("at", AT)
            .query(Long.class)
            .single();
    }

    private long insertTerm(String table, String name, String slug) {
        return jdbcClient.sql("INSERT INTO " + table + " (name, slug) VALUES (:name, :slug) RETURNING id")
            .param("name", name)
            .param("slug", slug)
            .query(Long.class)
            .single();
    }

    private void tag(long publication, long tag) {
        jdbcClient.sql("INSERT INTO publication_tag (publication_id, tag_id) VALUES (:publication, :tag)")
            .param("publication", publication)
            .param("tag", tag)
            .update();
    }

    private void delete(String table, long id) {
        jdbcClient.sql("DELETE FROM " + table + " WHERE id = :id").param("id", id).update();
    }

    private long count(String table) {
        return jdbcClient.sql("SELECT count(*) FROM " + table).query(Long.class).single();
    }
}
