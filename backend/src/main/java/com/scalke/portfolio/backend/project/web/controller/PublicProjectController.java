package com.scalke.portfolio.backend.project.web.controller;

import com.scalke.portfolio.backend.project.application.usecase.GetPublishedProjectUseCase;
import com.scalke.portfolio.backend.project.application.usecase.ListPublishedProjectsUseCase;
import com.scalke.portfolio.backend.project.domain.model.ProjectFilter;
import com.scalke.portfolio.backend.project.web.dto.ProjectResponse;
import com.scalke.portfolio.backend.project.web.dto.ProjectSummaryResponse;
import com.scalke.portfolio.backend.shared.api.ApiPaging;
import com.scalke.portfolio.backend.shared.api.PageResponse;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/public/projects")
public class PublicProjectController {

    private final ListPublishedProjectsUseCase listPublishedProjectsUseCase;
    private final GetPublishedProjectUseCase getPublishedProjectUseCase;

    /**
     * {@code page} (0-based) et {@code size} sont bornés par Spring Data avant d'arriver ici :
     * taille par défaut {@link ApiPaging#PUBLIC_PAGE_SIZE}, maximum {@code spring.data.web.pageable.max-page-size}.
     * L'ordre est fixe (D-V) : un paramètre {@code sort} est ignoré.
     * <p>
     * {@code technology} : slug d'une technologie ; absent ou vide, aucun filtre ; inconnu, page vide (D-AC).
     */
    @GetMapping
    PageResponse<ProjectSummaryResponse> listProjects(
        @RequestParam(required = false) String technology,
        @PageableDefault(size = ApiPaging.PUBLIC_PAGE_SIZE) Pageable pageable) {
        PageQuery query = new PageQuery(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.from(listPublishedProjectsUseCase
            .execute(ProjectFilter.byTechnology(technology), query)
            .map(ProjectSummaryResponse::from));
    }

    @GetMapping("/{slug}")
    ProjectResponse getProject(@PathVariable String slug) {
        return ProjectResponse.from(getPublishedProjectUseCase.execute(slug));
    }
}
