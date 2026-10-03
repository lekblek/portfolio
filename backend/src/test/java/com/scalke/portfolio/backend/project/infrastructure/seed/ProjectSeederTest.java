package com.scalke.portfolio.backend.project.infrastructure.seed;

import com.scalke.portfolio.backend.media.application.query.MediaQueryService;
import com.scalke.portfolio.backend.project.application.usecase.CreateProjectUseCase;
import com.scalke.portfolio.backend.project.application.usecase.ProjectDraft;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.shared.api.ApiPaging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * D-EU : le jeu de projets de démonstration franchit deux pages publiques et deux pages d'administration, couvre
 * chaque état, et ne recrée rien d'existant.
 */
class ProjectSeederTest {

    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final TechnologyRepository technologies = mock(TechnologyRepository.class);
    private final CreateProjectUseCase createProjectUseCase = mock(CreateProjectUseCase.class);
    private final ProjectSeeder seeder = new ProjectSeeder(projects, technologies, createProjectUseCase,
        mock(MediaQueryService.class), JsonMapper.builder().build());

    @BeforeEach
    void createTechnologies() {
        AtomicLong ids = new AtomicLong();
        when(technologies.create(any())).thenAnswer(call -> {
            Technology technology = call.getArgument(0);
            return new Technology(ids.incrementAndGet(), technology.name(), technology.slug(), technology.displayOrder());
        });
    }

    @Test
    void fills_two_pages_of_each_list_in_every_state() {
        seeder.run(null);

        ArgumentCaptor<ProjectDraft> captor = ArgumentCaptor.forClass(ProjectDraft.class);
        verify(createProjectUseCase, atLeastOnce()).execute(isNull(), captor.capture());
        List<ProjectDraft> drafts = captor.getAllValues();
        assertThat(drafts).hasSizeGreaterThan(ApiPaging.ADMIN_PAGE_SIZE);
        assertThat(drafts.stream().filter(draft -> draft.visibility() == ProjectVisibility.PUBLISHED))
            .hasSizeGreaterThan(ApiPaging.PUBLIC_PAGE_SIZE);
        assertThat(drafts).extracting(ProjectDraft::visibility).contains(ProjectVisibility.values());
        assertThat(drafts).extracting(ProjectDraft::stage).contains(ProjectStage.values());
        assertThat(drafts).anyMatch(ProjectDraft::featured).anyMatch(draft -> !draft.featured());
        assertThat(drafts).allSatisfy(draft ->
            assertThat(draft.stage() == ProjectStage.IN_PROGRESS).isEqualTo(draft.endDate() == null));
    }

    @Test
    void creates_nothing_that_already_exists() {
        when(projects.existsBySlug(any(), isNull())).thenReturn(true);

        seeder.run(null);

        verify(createProjectUseCase, never()).execute(any(), any());
    }
}
