package com.scalke.portfolio.backend.series;

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
 * Contraintes de {@code V011__create_series.sql}, vérifiées sans JPA : invariants 1 à 3 du modèle (D05)
 * et position strictement positive.
 */
@Transactional
class SeriesSchemaIT extends AbstractIntegrationTest {

    private static final OffsetDateTime AT = OffsetDateTime.parse("2026-06-01T09:00:00Z");

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void rejects_a_duplicated_slug() {
        insertSeries("spring-boot");

        assertThatThrownBy(() -> insertSeries("spring-boot"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("series_slug_unique");
    }

    @Test
    void rejects_a_slug_that_is_not_url_friendly() {
        assertThatThrownBy(() -> insertSeries("Spring Boot"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("series_slug_format_check");
    }

    @Test
    void orders_articles_by_position() {
        long series = insertSeries("spring-boot");
        long first = insertPublication("premier", "ARTICLE");
        long second = insertPublication("second", "ARTICLE");

        insertItem(series, second, 2);
        insertItem(series, first, 1);

        assertThat(jdbcClient.sql("SELECT publication_id FROM series_item WHERE series_id = :series ORDER BY position")
            .param("series", series)
            .query(Long.class)
            .list())
            .containsExactly(first, second);
    }

    /**
     * Invariant 1 : la clé étrangère composite {@code (publication_id, publication_type)} ne trouve aucune
     * publication {@code (id, 'ARTICLE')} pour une NEWS.
     */
    @Test
    void rejects_a_news() {
        long series = insertSeries("spring-boot");
        long news = insertPublication("annonce", "NEWS");

        assertThatThrownBy(() -> insertItem(series, news, 1))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("series_item_publication_fk");
    }

    @Test
    void rejects_an_item_declared_as_something_else_than_an_article() {
        long series = insertSeries("spring-boot");
        long news = insertPublication("annonce", "NEWS");

        assertThatThrownBy(() -> jdbcClient.sql("""
                    INSERT INTO series_item (series_id, publication_id, publication_type, position)
                    VALUES (:series, :publication, 'NEWS', 1)
                    """)
            .param("series", series)
            .param("publication", news)
            .update())
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("series_item_article_only_check");
    }

    /**
     * Le module {@code publication} ignore les séries : seule la base empêche un article d'une série de
     * devenir une NEWS (D-BF).
     */
    @Test
    void refuses_to_turn_an_article_of_a_series_into_a_news() {
        long series = insertSeries("spring-boot");
        long article = insertPublication("chapitre", "ARTICLE");
        insertItem(series, article, 1);

        assertThatThrownBy(() -> jdbcClient.sql("UPDATE publication SET type = 'NEWS' WHERE id = :id")
            .param("id", article)
            .update())
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("series_item_publication_fk");
    }

    @Test
    void rejects_an_article_in_two_series() {
        long article = insertPublication("chapitre", "ARTICLE");
        insertItem(insertSeries("spring-boot"), article, 1);
        long other = insertSeries("angular");

        assertThatThrownBy(() -> insertItem(other, article, 1))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("series_item_publication_unique");
    }

    @Test
    void rejects_two_articles_at_the_same_position() {
        long series = insertSeries("spring-boot");
        insertItem(series, insertPublication("premier", "ARTICLE"), 1);
        long second = insertPublication("second", "ARTICLE");

        assertThatThrownBy(() -> insertItem(series, second, 1))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("series_item_pk");
    }

    @Test
    void rejects_a_position_that_is_not_positive() {
        long series = insertSeries("spring-boot");
        long article = insertPublication("chapitre", "ARTICLE");

        assertThatThrownBy(() -> insertItem(series, article, 0))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("series_item_position_check");
    }

    @Test
    void deleting_a_series_keeps_its_articles() {
        long series = insertSeries("spring-boot");
        insertItem(series, insertPublication("chapitre", "ARTICLE"), 1);

        jdbcClient.sql("DELETE FROM series WHERE id = :id").param("id", series).update();

        assertThat(count("series_item")).isZero();
        assertThat(count("publication")).isEqualTo(1);
    }

    @Test
    void deleting_an_article_removes_it_from_its_series() {
        long series = insertSeries("spring-boot");
        long article = insertPublication("chapitre", "ARTICLE");
        insertItem(series, article, 1);

        jdbcClient.sql("DELETE FROM publication WHERE id = :id").param("id", article).update();

        assertThat(count("series_item")).isZero();
        assertThat(count("series")).isEqualTo(1);
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
        long series = insertSeries("spring-boot");
        jdbcClient.sql("UPDATE series SET cover_media_id = :media WHERE id = :series")
            .param("media", media)
            .param("series", series)
            .update();

        assertThatThrownBy(() -> jdbcClient.sql("DELETE FROM media WHERE id = :media").param("media", media).update())
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("series_cover_media_fk");
    }

    private long insertSeries(String slug) {
        return jdbcClient.sql("""
                    INSERT INTO series (title, slug, description_markdown)
                    VALUES ('Série', :slug, 'Description.')
                    RETURNING id
                    """)
            .param("slug", slug)
            .query(Long.class)
            .single();
    }

    private long insertPublication(String slug, String type) {
        return jdbcClient.sql("""
                    INSERT INTO publication (type, title, slug, summary, content_markdown, status,
                                             created_at, updated_at)
                    VALUES (:type, 'Titre', :slug, 'Résumé', '# Contenu', 'DRAFT', :at, :at)
                    RETURNING id
                    """)
            .param("type", type)
            .param("slug", slug)
            .param("at", AT)
            .query(Long.class)
            .single();
    }

    private void insertItem(long series, long publication, int position) {
        jdbcClient.sql("""
                    INSERT INTO series_item (series_id, publication_id, position)
                    VALUES (:series, :publication, :position)
                    """)
            .param("series", series)
            .param("publication", publication)
            .param("position", position)
            .update();
    }

    private long count(String table) {
        return jdbcClient.sql("SELECT count(*) FROM " + table).query(Long.class).single();
    }
}
