package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.scalke.portfolio.backend.project.ProjectFixtures.project;
import static com.scalke.portfolio.backend.project.ProjectFixtures.published;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ListPublishedProjectsUseCaseIT extends AbstractIntegrationTest {

    @Autowired
    ListPublishedProjectsUseCase listPublishedProjectsUseCase;

    @Autowired
    ProjectRepository projectRepository;

    @Test
    void returns_an_empty_page_when_nothing_is_published() {
        projectRepository.create(project("brouillon", ProjectVisibility.DRAFT,
            DateRange.ongoingSince(LocalDate.of(2025, 1, 1)), 0));

        PageResult<Project> page = listPublishedProjectsUseCase.execute(new PageQuery(0, 10));

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

        PageResult<Project> page = listPublishedProjectsUseCase.execute(new PageQuery(0, 10));

        assertThat(page.content())
            .extracting(Project::slug)
            .containsExactly("recent-a", "recent-b", "ancien", "mis-en-retrait");
        assertThat(page.totalElements()).isEqualTo(4);
    }

    @Test
    void paginates_published_projects_only() {
        givenProjectsOfEveryVisibility();

        PageResult<Project> page = listPublishedProjectsUseCase.execute(new PageQuery(1, 3));

        assertThat(page.content()).extracting(Project::slug).containsExactly("mis-en-retrait");
        assertThat(page.totalElements()).isEqualTo(4);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.isLast()).isTrue();
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
