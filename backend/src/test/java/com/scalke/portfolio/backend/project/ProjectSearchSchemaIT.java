package com.scalke.portfolio.backend.project;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Document de recherche des projets ({@code V019}, D-CA, D-CB), vérifié sans JPA : pondération, suivi du
 * texte et des technologies (invariant 28), index GIN.
 */
@Transactional
class ProjectSearchSchemaIT extends AbstractIntegrationTest {

    @Autowired
    JdbcClient jdbcClient;

    private long project;

    @BeforeEach
    void givenAProject() {
        project = insertProject("kubernetes", "Kubernetes en pratique", "Déployer avec Ansible",
            "# Surveiller\n\nAvec Prometheus.");
    }

    /**
     * Même pondération que les publications : titre et technologies A, description courte B, description D.
     */
    @Test
    void weights_the_title_and_the_technologies_above_the_short_description_and_it_above_the_description() {
        link(project, insertTechnology("Terraform", "terraform"));

        assertThat(weightOf("kubernetes")).isEqualTo("A");
        assertThat(weightOf("terraform")).isEqualTo("A");
        assertThat(weightOf("ansible")).isEqualTo("B");
        assertThat(weightOf("prometheus")).isEqualTo("D");
    }

    @Test
    void follows_the_text_of_the_project() {
        jdbcClient.sql("UPDATE project SET short_description = 'Déployer avec Helm' WHERE id = :id")
            .param("id", project)
            .update();

        assertThat(matches("helm")).isTrue();
        assertThat(matches("ansible")).isFalse();
    }

    @Test
    void copies_the_names_of_its_technologies_in_alphabetical_order() {
        link(project, insertTechnology("Spring Boot", "spring-boot"));
        link(project, insertTechnology("Java", "java"));

        assertThat(technologyNames(project)).isEqualTo("Java Spring Boot");
        assertThat(matches("java")).isTrue();
    }

    @Test
    void forgets_a_technology_removed_from_the_project() {
        long terraform = insertTechnology("Terraform", "terraform");
        link(project, terraform);

        jdbcClient.sql("DELETE FROM project_technology WHERE technology_id = :technology")
            .param("technology", terraform)
            .update();

        assertThat(technologyNames(project)).isEmpty();
        assertThat(matches("terraform")).isFalse();
    }

    @Test
    void follows_the_renaming_of_a_technology_in_the_projects_that_use_it() {
        long other = insertProject("autre", "Titre", "Résumé", "# Description");
        long terraform = insertTechnology("Terraform", "terraform");
        link(project, terraform);
        link(other, insertTechnology("Vault", "vault"));

        jdbcClient.sql("UPDATE technology SET name = 'OpenTofu' WHERE id = :id").param("id", terraform).update();

        assertThat(technologyNames(project)).isEqualTo("OpenTofu");
        assertThat(matches("opentofu")).isTrue();
        assertThat(technologyNames(other)).isEqualTo("Vault");
    }

    @Test
    void prevents_dropping_the_technology_name_the_document_depends_on() {
        assertThatThrownBy(() -> jdbcClient.sql("ALTER TABLE technology DROP COLUMN name").update())
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("project_technology_names");
    }

    @Test
    void filters_through_the_gin_index() {
        jdbcClient.sql("SET LOCAL enable_seqscan = off").update();

        List<String> plan = jdbcClient.sql("""
                    EXPLAIN SELECT id FROM project
                     WHERE search_vector @@ websearch_to_tsquery('french_unaccent', 'kubernetes')
                    """)
            .query(String.class)
            .list();

        assertThat(String.join("\n", plan)).contains("Bitmap Index Scan on project_search_vector_idx");
    }

    private String weightOf(String word) {
        return jdbcClient.sql("""
                    SELECT w FROM project, unnest(ARRAY['A', 'B', 'C', 'D']) AS w
                     WHERE id = :id
                       AND ts_filter(search_vector, ARRAY[lower(w)]::"char"[]) @@ websearch_to_tsquery('french_unaccent', :word)
                    """)
            .param("id", project)
            .param("word", word)
            .query(String.class)
            .single();
    }

    private boolean matches(String search) {
        return jdbcClient.sql("""
                    SELECT search_vector @@ websearch_to_tsquery('french_unaccent', :search) FROM project WHERE id = :id
                    """)
            .param("id", project)
            .param("search", search)
            .query(Boolean.class)
            .single();
    }

    private String technologyNames(long id) {
        return jdbcClient.sql("SELECT technology_names FROM project WHERE id = :id")
            .param("id", id)
            .query(String.class)
            .single();
    }

    private long insertProject(String slug, String title, String shortDescription, String description) {
        return jdbcClient.sql("""
                    INSERT INTO project (title, slug, short_description, description_markdown,
                                         stage, visibility, start_date)
                    VALUES (:title, :slug, :shortDescription, :description, 'IN_PROGRESS', 'DRAFT', DATE '2024-01-01')
                    RETURNING id
                    """)
            .param("title", title)
            .param("slug", slug)
            .param("shortDescription", shortDescription)
            .param("description", description)
            .query(Long.class)
            .single();
    }

    private long insertTechnology(String name, String slug) {
        return jdbcClient.sql("INSERT INTO technology (name, slug, display_order) VALUES (:name, :slug, 0) RETURNING id")
            .param("name", name)
            .param("slug", slug)
            .query(Long.class)
            .single();
    }

    private void link(long projectId, long technologyId) {
        jdbcClient.sql("INSERT INTO project_technology (project_id, technology_id) VALUES (:project, :technology)")
            .param("project", projectId)
            .param("technology", technologyId)
            .update();
    }
}
