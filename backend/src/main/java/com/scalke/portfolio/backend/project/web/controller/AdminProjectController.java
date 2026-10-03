package com.scalke.portfolio.backend.project.web.controller;

import com.scalke.portfolio.backend.project.application.usecase.CreateProjectUseCase;
import com.scalke.portfolio.backend.project.application.usecase.GetProjectUseCase;
import com.scalke.portfolio.backend.project.application.usecase.ListProjectsUseCase;
import com.scalke.portfolio.backend.project.application.usecase.UpdateProjectUseCase;
import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectAdminFilter;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.web.dto.AdminProjectResponse;
import com.scalke.portfolio.backend.project.web.dto.AdminProjectSummaryResponse;
import com.scalke.portfolio.backend.project.web.dto.SaveProjectRequest;
import com.scalke.portfolio.backend.shared.api.ApiPaging;
import com.scalke.portfolio.backend.shared.api.PageResponse;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Administration des projets (D-CX), derrière la session de l'administrateur : toute la saisie, visibilité comprise,
 * par une seule requête. Pas de suppression : l'archivage retire un projet du site.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/projects")
public class AdminProjectController {

    private final ListProjectsUseCase listProjectsUseCase;
    private final GetProjectUseCase getProjectUseCase;
    private final CreateProjectUseCase createProjectUseCase;
    private final UpdateProjectUseCase updateProjectUseCase;

    /**
     * {@code visibility} : {@code DRAFT}, {@code PUBLISHED} ou {@code ARCHIVED} ; absente, toutes. {@code technology} :
     * slug d'une technologie ; absent ou vide, toutes ; inconnu, page vide (F27).
     */
    @GetMapping
    PageResponse<AdminProjectSummaryResponse> list(
        @RequestParam(required = false) ProjectVisibility visibility,
        @RequestParam(required = false) String technology,
        @PageableDefault(size = ApiPaging.ADMIN_PAGE_SIZE) Pageable pageable) {
        PageQuery query = new PageQuery(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.from(listProjectsUseCase
            .execute(ProjectAdminFilter.of(visibility, technology), query)
            .map(AdminProjectSummaryResponse::from));
    }

    @GetMapping("/{id}")
    AdminProjectResponse get(@PathVariable Long id) {
        return AdminProjectResponse.from(getProjectUseCase.execute(id));
    }

    @PostMapping
    ResponseEntity<AdminProjectResponse> create(@Valid @RequestBody SaveProjectRequest body) {
        Project created = createProjectUseCase.execute(body.slug(), body.toDraft());
        return ResponseEntity
            .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri())
            .body(AdminProjectResponse.from(created));
    }

    @PutMapping("/{id}")
    AdminProjectResponse update(@PathVariable Long id, @Valid @RequestBody SaveProjectRequest body) {
        return AdminProjectResponse.from(updateProjectUseCase.execute(id, body.slug(), body.toDraft()));
    }
}
