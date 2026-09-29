package com.scalke.portfolio.backend.publication;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Document de recherche des publications ({@code V018}, D-CA, D-CB), vérifié sans JPA : pondération, suivi
 * du texte et des tags (invariant 28), index GIN.
 */
@Transactional
class PublicationSearchSchemaIT extends AbstractIntegrationTest {

    private static final OffsetDateTime AT = OffsetDateTime.parse("2026-06-01T09:00:00Z");

    @Autowired
    JdbcClient jdbcClient;

    private long publication;

    @BeforeEach
    void givenAPublication() {
        publication = jdbcClient.sql("""
                    INSERT INTO publication (type, title, slug, summary, content_markdown, status,
                                             created_at, updated_at)
                    VALUES ('ARTICLE', 'Kubernetes en pratique', 'kubernetes', 'Déployer avec Ansible',
                            '# Surveiller\n\nAvec Prometheus.', 'DRAFT', :at, :at)
                    RETURNING id
                    """)
            .param("at", AT)
            .query(Long.class)
            .single();
    }

    /**
     * 01 §11 : titre et tags A (forte), résumé B (moyenne), contenu D (normale).
     */
    @Test
    void weights_the_title_and_the_tags_above_the_summary_and_the_summary_above_the_content() {
        tag(publication, insertTag("Terraform", "terraform"));

        assertThat(weightOf("kubernetes")).isEqualTo("A");
        assertThat(weightOf("terraform")).isEqualTo("A");
        assertThat(weightOf("ansible")).isEqualTo("B");
        assertThat(weightOf("prometheus")).isEqualTo("D");
    }

    @Test
    void follows_the_text_of_the_publication() {
        jdbcClient.sql("UPDATE publication SET title = 'Helm en pratique' WHERE id = :id").param("id", publication).update();

        assertThat(matches("helm")).isTrue();
        assertThat(matches("kubernetes")).isFalse();
    }

    @Test
    void copies_the_names_of_its_tags_in_alphabetical_order() {
        tag(publication, insertTag("Spring Boot", "spring-boot"));
        tag(publication, insertTag("PostgreSQL", "postgresql"));

        assertThat(tagNames(publication)).isEqualTo("PostgreSQL Spring Boot");
        assertThat(matches("\"spring boot\"")).isTrue();
    }

    @Test
    void forgets_a_tag_removed_from_the_publication() {
        long terraform = insertTag("Terraform", "terraform");
        tag(publication, terraform);

        jdbcClient.sql("DELETE FROM publication_tag WHERE tag_id = :tag").param("tag", terraform).update();

        assertThat(tagNames(publication)).isEmpty();
        assertThat(matches("terraform")).isFalse();
    }

    /**
     * Le renommage a lieu dans le module taxonomy, qui ignore les publications : c'est PostgreSQL qui propage
     * le nouveau nom, aux seules publications qui portent le tag.
     */
    @Test
    void follows_the_renaming_of_a_tag_in_the_publications_that_carry_it() {
        long other = insertPublication("autre");
        long terraform = insertTag("Terraform", "terraform");
        tag(publication, terraform);
        tag(other, insertTag("Vault", "vault"));

        jdbcClient.sql("UPDATE tag SET name = 'OpenTofu' WHERE id = :id").param("id", terraform).update();

        assertThat(tagNames(publication)).isEqualTo("OpenTofu");
        assertThat(matches("opentofu")).isTrue();
        assertThat(matches("terraform")).isFalse();
        assertThat(tagNames(other)).isEqualTo("Vault");
    }

    /**
     * La dépendance de ce document à {@code tag.name} est enregistrée par PostgreSQL (corps SQL standard) :
     * une migration du module taxonomy ne peut pas supprimer la colonne sans le traiter.
     */
    @Test
    void prevents_dropping_the_tag_name_the_document_depends_on() {
        assertThatThrownBy(() -> jdbcClient.sql("ALTER TABLE tag DROP COLUMN name").update())
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("publication_tag_names");
    }

    @Test
    void filters_through_the_gin_index() {
        jdbcClient.sql("SET LOCAL enable_seqscan = off").update();

        List<String> plan = jdbcClient.sql("""
                    EXPLAIN SELECT id FROM publication
                     WHERE search_vector @@ websearch_to_tsquery('french_unaccent', 'kubernetes')
                    """)
            .query(String.class)
            .list();

        assertThat(String.join("\n", plan)).contains("Bitmap Index Scan on publication_search_vector_idx");
    }

    private String weightOf(String word) {
        return jdbcClient.sql("""
                    SELECT w FROM publication, unnest(ARRAY['A', 'B', 'C', 'D']) AS w
                     WHERE id = :id
                       AND ts_filter(search_vector, ARRAY[lower(w)]::"char"[]) @@ websearch_to_tsquery('french_unaccent', :word)
                    """)
            .param("id", publication)
            .param("word", word)
            .query(String.class)
            .single();
    }

    private boolean matches(String search) {
        return jdbcClient.sql("""
                    SELECT search_vector @@ websearch_to_tsquery('french_unaccent', :search) FROM publication WHERE id = :id
                    """)
            .param("id", publication)
            .param("search", search)
            .query(Boolean.class)
            .single();
    }

    private String tagNames(long id) {
        return jdbcClient.sql("SELECT tag_names FROM publication WHERE id = :id").param("id", id).query(String.class).single();
    }

    private long insertPublication(String slug) {
        return jdbcClient.sql("""
                    INSERT INTO publication (type, title, slug, summary, content_markdown, status,
                                             created_at, updated_at)
                    VALUES ('ARTICLE', 'Titre', :slug, 'Résumé', '# Contenu', 'DRAFT', :at, :at)
                    RETURNING id
                    """)
            .param("slug", slug)
            .param("at", AT)
            .query(Long.class)
            .single();
    }

    private long insertTag(String name, String slug) {
        return jdbcClient.sql("INSERT INTO tag (name, slug) VALUES (:name, :slug) RETURNING id")
            .param("name", name)
            .param("slug", slug)
            .query(Long.class)
            .single();
    }

    private void tag(long publicationId, long tagId) {
        jdbcClient.sql("INSERT INTO publication_tag (publication_id, tag_id) VALUES (:publication, :tag)")
            .param("publication", publicationId)
            .param("tag", tagId)
            .update();
    }
}
