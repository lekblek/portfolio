package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.scalke.portfolio.backend.project.ProjectFixtures.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class GetPublishedProjectUseCaseIT extends AbstractIntegrationTest {

    @Autowired
    GetPublishedProjectUseCase getPublishedProjectUseCase;

    @Autowired
    ProjectRepository projectRepository;

    @Test
    void returns_a_published_project_with_every_field() {
        Project stored = projectRepository.create(new Project(
            null, "Portfolio full-stack", "portfolio-full-stack", "Résumé", "## Description",
            ProjectStage.COMPLETED, ProjectVisibility.PUBLISHED,
            DateRange.between(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)),
            "https://example.test/repo", null, true, 0));

        Project project = getPublishedProjectUseCase.execute("portfolio-full-stack");

        assertThat(project).isEqualTo(stored);
        assertThat(project.id()).isNotNull();
        assertThat(project.demoUrl()).isNull();
    }

    /**
     * D-U : un projet non publié est indiscernable d'un projet inexistant.
     */
    @ParameterizedTest
    @ValueSource(strings = {"brouillon", "archive", "inconnu"})
    void fails_for_a_project_that_is_not_published(String slug) {
        projectRepository.create(project("brouillon", ProjectVisibility.DRAFT,
            DateRange.ongoingSince(LocalDate.of(2026, 1, 1)), 0));
        projectRepository.create(project("archive", ProjectVisibility.ARCHIVED,
            DateRange.between(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1)), 0));

        assertThatThrownBy(() -> getPublishedProjectUseCase.execute(slug))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Projet introuvable.");
    }
}
