package com.scalke.portfolio.backend.project;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.scalke.portfolio.backend.project.ProjectFixtures.project;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours HTTP complet : contrôleur → cas d'usage → adaptateur JPA → PostgreSQL.
 * Vérifie la sérialisation réelle et l'invisibilité des projets non publiés (invariant 11, D-U).
 */
@Transactional
class PublicProjectIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProjectRepository projectRepository;

    @BeforeEach
    void givenOnePublishedOneDraftAndOneArchivedProject() {
        projectRepository.create(new Project(
            null, "Portfolio full-stack", "portfolio-full-stack", "Résumé", "## Description",
            ProjectStage.COMPLETED, ProjectVisibility.PUBLISHED,
            DateRange.between(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)),
            null, "https://example.test/demo", false, 0));
        projectRepository.create(project("brouillon", ProjectVisibility.DRAFT,
            DateRange.ongoingSince(LocalDate.of(2026, 1, 1)), 0));
        projectRepository.create(project("archive", ProjectVisibility.ARCHIVED,
            DateRange.between(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1)), 0));
    }

    @Test
    void lists_only_published_projects() throws Exception {
        mockMvc.perform(get("/api/public/projects").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].slug").value("portfolio-full-stack"))
            .andExpect(jsonPath("$.content[0].stage").value("COMPLETED"))
            .andExpect(jsonPath("$.content[0].endDate").value("2024-06-30"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void returns_a_published_project_by_slug() throws Exception {
        mockMvc.perform(get("/api/public/projects/portfolio-full-stack").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Portfolio full-stack"))
            .andExpect(jsonPath("$.startDate").value("2024-01-01"))
            .andExpect(jsonPath("$.repositoryUrl").hasJsonPath())
            .andExpect(jsonPath("$.repositoryUrl").value(nullValue()))
            .andExpect(jsonPath("$.demoUrl").value("https://example.test/demo"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"brouillon", "archive", "inconnu"})
    void hides_projects_that_are_not_published_behind_a_404(String slug) throws Exception {
        mockMvc.perform(get("/api/public/projects/" + slug).contextPath("/api"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.detail").value("Projet introuvable."));
    }
}
