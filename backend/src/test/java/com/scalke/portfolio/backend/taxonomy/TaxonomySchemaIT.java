package com.scalke.portfolio.backend.taxonomy;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contraintes de {@code V007__create_taxonomy.sql}, vérifiées sans JPA, pour les deux vocabulaires.
 */
@Transactional
class TaxonomySchemaIT extends AbstractIntegrationTest {

    @Autowired
    JdbcClient jdbcClient;

    @ParameterizedTest
    @ValueSource(strings = {"category", "tag"})
    void rejects_a_duplicated_slug(String table) {
        insert(table, "Spring Boot", "spring-boot");

        assertThatThrownBy(() -> insert(table, "Spring", "spring-boot"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining(table + "_slug_unique");
    }

    @ParameterizedTest
    @ValueSource(strings = {"category", "tag"})
    void rejects_a_name_differing_only_by_case(String table) {
        insert(table, "Backend", "backend");

        assertThatThrownBy(() -> insert(table, "BACKEND", "backend-2"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining(table + "_name_unique_idx");
    }

    @ParameterizedTest
    @ValueSource(strings = {"category", "tag"})
    void rejects_a_slug_that_is_not_url_friendly(String table) {
        assertThatThrownBy(() -> insert(table, "Spring Boot", "Spring_Boot"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining(table + "_slug_format_check");
    }

    private void insert(String table, String name, String slug) {
        jdbcClient.sql("INSERT INTO " + table + " (name, slug) VALUES (:name, :slug)")
            .param("name", name)
            .param("slug", slug)
            .update();
    }
}
