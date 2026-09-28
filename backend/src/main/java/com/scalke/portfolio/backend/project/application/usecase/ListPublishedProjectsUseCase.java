package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectFilter;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListPublishedProjectsUseCase {

    private final ProjectRepository projectRepository;
    private final PublishedProjectAssembler assembler;

    /**
     * Projets publiés et leur couverture (D-BW) : une requête de plus pour toute la page, aucune si aucun projet
     * de la page n'a de couverture.
     */
    @Transactional(readOnly = true)
    public PageResult<PublishedProject> execute(ProjectFilter filter, PageQuery query) {
        PageResult<Project> page = projectRepository.findPublished(filter, query);
        return new PageResult<>(assembler.withCovers(page.content()), page.page(), page.size(), page.totalElements());
    }
}
