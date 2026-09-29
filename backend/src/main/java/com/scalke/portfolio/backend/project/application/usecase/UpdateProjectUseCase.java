package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectContent;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remplace toute la saisie d'un projet, visibilité comprise (D-CX). Sans slug saisi, le slug est conservé ; un slug
 * saisi et déjà pris est suffixé (D-BD), puis refusé si le projet a déjà été publié ({@code SLUG_LOCKED}, D11).
 */
@Service
@RequiredArgsConstructor
public class UpdateProjectUseCase {

    private final ProjectRepository projectRepository;
    private final ProjectAdministration administration;

    /**
     * @param slug slug saisi ; {@code null} : slug actuel conservé
     */
    @Transactional
    public Project execute(Long id, String slug, ProjectDraft draft) {
        Project current = administration.find(id);
        ProjectContent content = administration.resolve(draft);
        Slug wanted = slug != null ? Slug.of(slug) : current.slug();
        Slug free = wanted.firstAvailable(candidate -> projectRepository.existsBySlug(candidate, id));
        return projectRepository.update(current.edit(free, content));
    }
}
