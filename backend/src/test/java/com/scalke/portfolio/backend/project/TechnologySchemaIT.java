package com.scalke.portfolio.backend.project;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contraintes de {@code V005__create_technology.sql}, vérifiées sans JPA.
 */
@Transactional
class TechnologySchemaIT extends AbstractIntegrationTest {

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void rejects_a_duplicated_slug() {
        insertTechnology("Java", "java");

        assertThatThrownBy(() -> insertTechnology("Java SE", "java"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("technology_slug_unique");
    }

    @Test
    void rejects_a_name_differing_only_by_case() {
        insertTechnology("Java", "java");

        assertThatThrownBy(() -> insertTechnology("JAVA", "java-se"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("technology_name_unique_idx");
    }

    @Test
    void rejects_a_slug_that_is_not_url_friendly() {
        assertThatThrownBy(() -> insertTechnology("Spring Boot", "Spring Boot"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("technology_slug_format_check");
    }

    @Test
    void rejects_the_same_technology_twice_in_a_project() {
        long project = insertProject();
        long java = insertTechnology("Java", "java");
        link(project, java);

        assertThatThrownBy(() -> link(project, java))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("project_technology_pk");
    }

    @Test
    void refuses_to_delete_a_technology_used_by_a_project() {
        long java = insertTechnology("Java", "java");
        link(insertProject(), java);

        assertThatThrownBy(() -> jdbcClient.sql("DELETE FROM technology WHERE id = :id").param("id", java).update())
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("project_technology_technology_fk");
    }

    @Test
    void deleting_a_project_removes_its_links_but_keeps_the_technology() {
        long project = insertProject();
        link(project, insertTechnology("Java", "java"));

        jdbcClient.sql("DELETE FROM project WHERE id = :id").param("id", project).update();

        assertThat(count("project_technology")).isZero();
        assertThat(count("technology")).isEqualTo(1);
    }

    private long insertTechnology(String name, String slug) {
        return jdbcClient.sql("""
                    INSERT INTO technology (name, slug, display_order)
                    VALUES (:name, :slug, 0)
                    RETURNING id
                    """)
            .param("name", name)
            .param("slug", slug)
            .query(Long.class)
            .single();
    }

    private long insertProject() {
        return jdbcClient.sql("""
                    INSERT INTO project (title, slug, short_description, description_markdown,
                                         stage, visibility, start_date)
                    VALUES ('Titre', 'portfolio', 'Résumé', '# Description', 'IN_PROGRESS', 'PUBLISHED', DATE '2024-01-01')
                    RETURNING id
                    """)
            .query(Long.class)
            .single();
    }

    private void link(long project, long technology) {
        jdbcClient.sql("INSERT INTO project_technology (project_id, technology_id) VALUES (:project, :technology)")
            .param("project", project)
            .param("technology", technology)
            .update();
    }

    private long count(String table) {
        return jdbcClient.sql("SELECT count(*) FROM " + table).query(Long.class).single();
    }
}
