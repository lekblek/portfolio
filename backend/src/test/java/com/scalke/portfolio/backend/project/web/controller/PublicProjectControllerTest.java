package com.scalke.portfolio.backend.project.web.controller;

import com.scalke.portfolio.backend.project.application.usecase.GetPublishedProjectUseCase;
import com.scalke.portfolio.backend.project.application.usecase.ListPublishedProjectsUseCase;
import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectFilter;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.shared.api.ApiPaging;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicProjectController.class)
class PublicProjectControllerTest {

    private static final Project PORTFOLIO = new Project(
        42L, "Portfolio full-stack", "portfolio-full-stack", "Résumé", "## Description",
        ProjectStage.IN_PROGRESS, ProjectVisibility.PUBLISHED,
        DateRange.ongoingSince(LocalDate.of(2024, 1, 1)),
        "https://example.test/repo", null, true, 3,
        List.of(new Technology(7L, "Java", "java", 0), new Technology(8L, "Angular", "angular", 1)));

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ListPublishedProjectsUseCase listPublishedProjectsUseCase;

    @MockitoBean
    GetPublishedProjectUseCase getPublishedProjectUseCase;

    @Test
    void lists_projects_as_a_page_of_summaries() throws Exception {
        given(listPublishedProjectsUseCase.execute(any(), any()))
            .willReturn(new PageResult<>(List.of(PORTFOLIO), 0, 10, 1));

        mockMvc.perform(get("/api/public/projects").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].title").value("Portfolio full-stack"))
            .andExpect(jsonPath("$.content[0].slug").value("portfolio-full-stack"))
            .andExpect(jsonPath("$.content[0].stage").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.content[0].startDate").value("2024-01-01"))
            .andExpect(jsonPath("$.content[0].endDate").hasJsonPath())
            .andExpect(jsonPath("$.content[0].endDate").value(nullValue()))
            .andExpect(jsonPath("$.content[0].featured").value(true))
            .andExpect(jsonPath("$.content[0].technologies[0].name").value("Java"))
            .andExpect(jsonPath("$.content[0].technologies[0].slug").value("java"))
            .andExpect(jsonPath("$.content[0].technologies[1].slug").value("angular"))
            .andExpect(jsonPath("$.content[0].technologies[0].id").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].technologies[0].displayOrder").doesNotHaveJsonPath())
            // le détail n'est pas dans la liste
            .andExpect(jsonPath("$.content[0].descriptionMarkdown").doesNotHaveJsonPath())
            // aucun champ technique ni interne (C03, D-W)
            .andExpect(jsonPath("$.content[0].id").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].displayOrder").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].visibility").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].period").doesNotHaveJsonPath())
            // enveloppe de pagination (05-conventions-api.md §14)
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void uses_the_public_page_size_by_default() throws Exception {
        given(listPublishedProjectsUseCase.execute(any(), any())).willReturn(new PageResult<>(List.of(), 0, 10, 0));

        mockMvc.perform(get("/api/public/projects").contextPath("/api"))
            .andExpect(status().isOk());

        then(listPublishedProjectsUseCase).should()
            .execute(ProjectFilter.none(), new PageQuery(0, ApiPaging.PUBLIC_PAGE_SIZE));
    }

    /**
     * Garde-fou : {@code spring.data.web.pageable.max-page-size} doit rester égal à
     * {@link ApiPaging#MAX_PAGE_SIZE}.
     */
    @Test
    void caps_the_page_size_at_the_api_maximum() throws Exception {
        given(listPublishedProjectsUseCase.execute(any(), any())).willReturn(new PageResult<>(List.of(), 2, 100, 0));

        mockMvc.perform(get("/api/public/projects").contextPath("/api")
                .param("page", "2")
                .param("size", "1000000"))
            .andExpect(status().isOk());

        then(listPublishedProjectsUseCase).should()
            .execute(ProjectFilter.none(), new PageQuery(2, ApiPaging.MAX_PAGE_SIZE));
    }

    @Test
    void passes_the_technology_filter_to_the_use_case() throws Exception {
        given(listPublishedProjectsUseCase.execute(any(), any())).willReturn(new PageResult<>(List.of(), 0, 10, 0));

        mockMvc.perform(get("/api/public/projects").contextPath("/api").param("technology", "spring-boot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());

        then(listPublishedProjectsUseCase).should()
            .execute(ProjectFilter.byTechnology("spring-boot"), new PageQuery(0, ApiPaging.PUBLIC_PAGE_SIZE));
    }

    @Test
    void returns_the_project_detail_with_explicit_nulls() throws Exception {
        given(getPublishedProjectUseCase.execute("portfolio-full-stack")).willReturn(PORTFOLIO);

        mockMvc.perform(get("/api/public/projects/portfolio-full-stack").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("portfolio-full-stack"))
            .andExpect(jsonPath("$.descriptionMarkdown").value("## Description"))
            .andExpect(jsonPath("$.repositoryUrl").value("https://example.test/repo"))
            .andExpect(jsonPath("$.demoUrl").hasJsonPath())
            .andExpect(jsonPath("$.demoUrl").value(nullValue()))
            .andExpect(jsonPath("$.technologies[0].slug").value("java"))
            .andExpect(jsonPath("$.id").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.visibility").doesNotHaveJsonPath());
    }

    @Test
    void returns_problem_details_when_the_project_is_not_public() throws Exception {
        given(getPublishedProjectUseCase.execute("brouillon"))
            .willThrow(new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Projet introuvable."));

        mockMvc.perform(get("/api/public/projects/brouillon").contextPath("/api"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
