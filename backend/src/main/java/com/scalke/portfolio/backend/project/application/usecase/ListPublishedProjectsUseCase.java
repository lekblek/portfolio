package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.project.domain.model.Project;
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

    @Transactional(readOnly = true)
    public PageResult<Project> execute(PageQuery query) {
        return projectRepository.findPublished(query);
    }
}
