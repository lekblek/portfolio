package com.scalke.portfolio.backend.project;

import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contraintes de {@code V004__create_project.sql} et {@code V023} (mémoire de publication, longueur de la
 * description, D-CX), vérifiées sans JPA.
 */
@Transactional
class ProjectSchemaIT extends AbstractIntegrationTest {

    private static final LocalDate START = LocalDate.of(2024, 1, 1);
    private static final LocalDate END = LocalDate.of(2024, 6, 30);

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void accepts_an_ongoing_and_a_completed_project() {
        insertProject("portfolio", "IN_PROGRESS", "PUBLISHED", START, null);
        insertProject("scalke-v1", "COMPLETED", "DRAFT", START, END);

        long count = jdbcClient.sql("SELECT count(*) FROM project").query(Long.class).single();

        assertThat(count).isEqualTo(2);
    }

    @Test
    void rejects_a_duplicated_slug() {
        insertProject("portfolio", "IN_PROGRESS", "PUBLISHED", START, null);

        assertThatThrownBy(() -> insertProject("portfolio", "IN_PROGRESS", "DRAFT", START, null))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("project_slug_unique");
    }

    @Test
    void rejects_a_slug_that_is_not_url_friendly() {
        assertThatThrownBy(() -> insertProject("Mon Projet", "IN_PROGRESS", "DRAFT", START, null))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("project_slug_format_check");
    }

    @Test
    void rejects_an_unknown_visibility() {
        assertThatThrownBy(() -> insertProject("portfolio", "IN_PROGRESS", "PRIVATE", START, null))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("project_visibility_check");
    }

    @Test
    void rejects_a_project_ending_before_it_starts() {
        assertThatThrownBy(() -> insertProject("portfolio", "COMPLETED", "DRAFT", END, START))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("project_dates_check");
    }

    @Test
    void rejects_an_in_progress_project_with_an_end_date() {
        assertThatThrownBy(() -> insertProject("portfolio", "IN_PROGRESS", "DRAFT", START, END))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("project_stage_matches_dates_check");
    }

    @Test
    void rejects_a_completed_project_without_an_end_date() {
        assertThatThrownBy(() -> insertProject("portfolio", "COMPLETED", "DRAFT", START, null))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("project_stage_matches_dates_check");
    }

    /**
     * D-CX : un projet publié a toujours été publié ; un projet archivé ou en brouillon peut l'avoir été.
     */
    @Test
    void a_published_project_remembers_its_publication() {
        insertProject("brouillon", "IN_PROGRESS", "DRAFT", START, null, false);
        insertProject("archive", "IN_PROGRESS", "ARCHIVED", START, null, true);

        assertThatThrownBy(() -> insertProject("portfolio", "IN_PROGRESS", "PUBLISHED", START, null, false))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("project_ever_published_check");
    }

    /**
     * D-CX, KI-31 : la description est bornée à 100 000 caractères (et non octets).
     */
    @Test
    void bounds_the_description_length() {
        insertProject("portfolio", "IN_PROGRESS", "DRAFT", START, null);

        jdbcClient.sql("UPDATE project SET description_markdown = :text").param("text", "é".repeat(100_000)).update();
        assertThatThrownBy(() -> jdbcClient.sql("UPDATE project SET description_markdown = :text")
                .param("text", "é".repeat(100_001)).update())
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("project_description_length_check");
    }

    private void insertProject(String slug, String stage, String visibility, LocalDate start, LocalDate end) {
        insertProject(slug, stage, visibility, start, end, "PUBLISHED".equals(visibility));
    }

    private void insertProject(String slug, String stage, String visibility, LocalDate start, LocalDate end,
                               boolean everPublished) {
        jdbcClient.sql("""
                    INSERT INTO project (title, slug, short_description, description_markdown,
                                         stage, visibility, start_date, end_date, ever_published)
                    VALUES ('Titre', :slug, 'Résumé', '# Description', :stage, :visibility, :start, :end,
                            :everPublished)
                    """)
            .param("everPublished", everPublished)
            .param("slug", slug)
            .param("stage", stage)
            .param("visibility", visibility)
            .param("start", start)
            .param("end", end)
            .update();
    }
}
