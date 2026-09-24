package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectFilter;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.scalke.portfolio.backend.project.ProjectFixtures.project;
import static com.scalke.portfolio.backend.project.ProjectFixtures.published;
import static com.scalke.portfolio.backend.project.ProjectFixtures.technology;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ListPublishedProjectsUseCaseIT extends AbstractIntegrationTest {

    @Autowired
    ListPublishedProjectsUseCase listPublishedProjectsUseCase;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    TechnologyRepository technologyRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void returns_an_empty_page_when_nothing_is_published() {
        projectRepository.create(project("brouillon", ProjectVisibility.DRAFT,
            DateRange.ongoingSince(LocalDate.of(2025, 1, 1)), 0));

        PageResult<Project> page = listPublishedProjectsUseCase.execute(ProjectFilter.none(), new PageQuery(0, 10));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.isLast()).isTrue();
    }

    /**
     * Invariant 11 (D-U) et ordre public total : displayOrder, puis date de début décroissante,
     * puis ordre de création (identifiant) en cas d'égalité.
     */
    @Test
    void returns_only_published_projects_in_public_display_order() {
        givenProjectsOfEveryVisibility();

        PageResult<Project> page = listPublishedProjectsUseCase.execute(ProjectFilter.none(), new PageQuery(0, 10));

        assertThat(page.content())
            .extracting(Project::slug)
            .containsExactly("recent-a", "recent-b", "ancien", "mis-en-retrait");
        assertThat(page.totalElements()).isEqualTo(4);
    }

    @Test
    void paginates_published_projects_only() {
        givenProjectsOfEveryVisibility();

        PageResult<Project> page = listPublishedProjectsUseCase.execute(ProjectFilter.none(), new PageQuery(1, 3));

        assertThat(page.content()).extracting(Project::slug).containsExactly("mis-en-retrait");
        assertThat(page.totalElements()).isEqualTo(4);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void returns_each_project_with_its_technologies_in_vocabulary_order() {
        Technology java = technologyRepository.create(technology("Java", "java", 0));
        Technology angular = technologyRepository.create(technology("Angular", "angular", 1));
        projectRepository.create(published("portfolio", LocalDate.of(2025, 1, 1), 0, angular, java));
        projectRepository.create(published("sans-techno", LocalDate.of(2024, 1, 1), 0));
        entityManager.flush();
        entityManager.clear();

        PageResult<Project> page = listPublishedProjectsUseCase.execute(ProjectFilter.none(), new PageQuery(0, 10));

        assertThat(page.content().get(0).technologies()).extracting(Technology::slug).containsExactly("java", "angular");
        assertThat(page.content().get(1).technologies()).isEmpty();
    }

    /**
     * D-AC : seuls les projets publiés utilisant la technologie, comptés sans doublon.
     */
    @Test
    void filters_published_projects_by_technology() {
        Technology java = technologyRepository.create(technology("Java", "java", 0));
        Technology angular = technologyRepository.create(technology("Angular", "angular", 1));
        projectRepository.create(published("backend", LocalDate.of(2025, 1, 1), 0, java));
        projectRepository.create(published("full-stack", LocalDate.of(2024, 1, 1), 0, java, angular));
        projectRepository.create(published("frontend", LocalDate.of(2023, 1, 1), 0, angular));
        projectRepository.create(project("brouillon-java", ProjectVisibility.DRAFT,
            DateRange.ongoingSince(LocalDate.of(2026, 1, 1)), 0, java));

        PageResult<Project> page = listPublishedProjectsUseCase.execute(
            ProjectFilter.byTechnology("java"), new PageQuery(0, 10));

        assertThat(page.content()).extracting(Project::slug).containsExactly("backend", "full-stack");
        assertThat(page.totalElements()).isEqualTo(2);
        // le filtre restreint les projets, pas leurs technologies
        assertThat(page.content().get(1).technologies()).extracting(Technology::slug)
            .containsExactly("java", "angular");
    }

    @Test
    void an_unknown_technology_gives_an_empty_page() {
        projectRepository.create(published("backend", LocalDate.of(2025, 1, 1), 0,
            technologyRepository.create(technology("Java", "java", 0))));

        PageResult<Project> page = listPublishedProjectsUseCase.execute(
            ProjectFilter.byTechnology("cobol"), new PageQuery(0, 10));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    /**
     * D-AB : une page complète coûte 3 requêtes (projets, comptage, technologies de toute la page),
     * quel que soit le nombre de projets. Sans {@code @BatchSize}, il y aurait une requête de
     * technologies par projet (N+1).
     */
    @Test
    void loads_a_page_with_its_technologies_in_a_constant_number_of_queries() {
        Technology java = technologyRepository.create(technology("Java", "java", 0));
        Technology angular = technologyRepository.create(technology("Angular", "angular", 1));
        for (int i = 0; i < 4; i++) {
            projectRepository.create(published("projet-" + i, LocalDate.of(2020 + i, 1, 1), 0, java, angular));
        }
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        PageResult<Project> page = listPublishedProjectsUseCase.execute(ProjectFilter.none(), new PageQuery(0, 3));

        assertThat(page.content()).hasSize(3).allSatisfy(p -> assertThat(p.technologies()).hasSize(2));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }

    private void givenProjectsOfEveryVisibility() {
        projectRepository.create(published("mis-en-retrait", LocalDate.of(2025, 6, 1), 1));
        projectRepository.create(published("ancien", LocalDate.of(2023, 1, 1), 0));
        projectRepository.create(published("recent-a", LocalDate.of(2025, 1, 1), 0));
        projectRepository.create(published("recent-b", LocalDate.of(2025, 1, 1), 0));
        projectRepository.create(project("brouillon", ProjectVisibility.DRAFT,
            DateRange.ongoingSince(LocalDate.of(2026, 1, 1)), 0));
        projectRepository.create(project("archive", ProjectVisibility.ARCHIVED,
            DateRange.between(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1)), 0));
    }
}
