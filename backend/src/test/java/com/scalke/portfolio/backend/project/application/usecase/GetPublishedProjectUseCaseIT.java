package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectScreenshot;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.scalke.portfolio.backend.media.MediaFixtures.image;
import static com.scalke.portfolio.backend.media.MediaFixtures.pdf;
import static com.scalke.portfolio.backend.project.ProjectFixtures.project;
import static com.scalke.portfolio.backend.project.ProjectFixtures.technology;
import static com.scalke.portfolio.backend.project.ProjectFixtures.withImages;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@Transactional
class GetPublishedProjectUseCaseIT extends AbstractIntegrationTest {

    @Autowired
    GetPublishedProjectUseCase getPublishedProjectUseCase;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    TechnologyRepository technologyRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    MediaRepository mediaRepository;

    @Test
    void returns_a_published_project_with_every_field_and_its_technologies() {
        Technology java = technologyRepository.create(technology("Java", "java", 0));
        Technology postgresql = technologyRepository.create(technology("PostgreSQL", "postgresql", 1));
        Project stored = projectRepository.create(new Project(
            null, "Portfolio full-stack", Slug.of("portfolio-full-stack"), "Résumé", "## Description",
            ProjectStage.COMPLETED, ProjectVisibility.PUBLISHED,
            DateRange.between(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)),
            "https://example.test/repo", null, true, 0,
            List.of(postgresql, java),
            null,
            List.of(), false));
        entityManager.flush();
        entityManager.clear();

        PublishedProject published = getPublishedProjectUseCase.execute("portfolio-full-stack");
        Project project = published.project();

        assertThat(project).isEqualTo(stored);
        assertThat(project.id()).isNotNull();
        assertThat(project.demoUrl()).isNull();
        assertThat(project.technologies()).containsExactly(java, postgresql);
    }

    /**
     * D-BW : couverture et captures sous forme publique, captures dans leur ordre d'affichage ; un média qui
     * n'est pas une image (PDF) n'est pas affiché.
     */
    @Test
    void returns_a_published_project_with_its_cover_and_screenshots_in_order() {
        Media cover = mediaRepository.create(image("Couverture"));
        Media home = mediaRepository.create(image("Accueil"));
        Media admin = mediaRepository.create(image("Administration"));
        Media document = mediaRepository.create(pdf());
        projectRepository.create(withImages(project("portfolio", ProjectVisibility.PUBLISHED,
                DateRange.ongoingSince(LocalDate.of(2026, 1, 1)), 0),
            cover.id(),
            new ProjectScreenshot(admin.id(), "Administration", 2),
            new ProjectScreenshot(document.id(), null, 0),
            new ProjectScreenshot(home.id(), "Page d'accueil", 1)));
        entityManager.flush();
        entityManager.clear();

        PublishedProject published = getPublishedProjectUseCase.execute("portfolio");

        assertThat(published.cover().url()).endsWith(cover.storageKey().value());
        assertThat(published.cover().width()).isEqualTo(1200);
        assertThat(published.screenshots())
            .extracting(PublishedScreenshot::caption, screenshot -> screenshot.image().altText())
            .containsExactly(tuple("Page d'accueil", "Accueil"), tuple("Administration", "Administration"));
    }

    /**
     * D-U : un projet non publié est indiscernable d'un projet inexistant ; un slug mal formé aussi (D-BB).
     */
    @ParameterizedTest
    @ValueSource(strings = {"brouillon", "archive", "inconnu", "Brouillon", "../brouillon"})
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
