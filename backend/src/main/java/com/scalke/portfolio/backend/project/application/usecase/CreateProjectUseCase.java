package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectContent;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crée un projet dans la visibilité saisie (D-CX). Slug saisi ou généré depuis le titre, puis premier libre (D-BD) ;
 * saisie vérifiée par {@link ProjectAdministration#resolve}.
 */
@Service
@RequiredArgsConstructor
public class CreateProjectUseCase {

    private final ProjectRepository projectRepository;
    private final ProjectAdministration administration;

    /**
     * @param slug slug saisi ; {@code null} : généré depuis le titre
     */
    @Transactional
    public Project execute(String slug, ProjectDraft draft) {
        ProjectContent content = administration.resolve(draft);
        Slug wanted = slug != null ? Slug.of(slug) : Slug.fromText(content.title());
        Slug free = wanted.firstAvailable(candidate -> projectRepository.existsBySlug(candidate, null));
        return projectRepository.create(Project.newProject(free, content));
    }
}
