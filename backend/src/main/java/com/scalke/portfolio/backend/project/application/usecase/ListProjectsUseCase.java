package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectAdminFilter;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projets de toute visibilité pour l'administration (D-CX), restreints par le filtre de la liste (visibilité,
 * technologie, F27), dans l'ordre d'affichage public.
 */
@Service
@RequiredArgsConstructor
public class ListProjectsUseCase {

    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public PageResult<Project> execute(ProjectAdminFilter filter, PageQuery query) {
        return projectRepository.findPage(filter, query);
    }
}
