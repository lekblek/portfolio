package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPublishedProjectUseCase {

    private final ProjectRepository projectRepository;

    /**
     * Un projet inexistant et un projet non publié produisent la même erreur : l'API publique ne
     * confirme pas l'existence d'un contenu invisible ({@code docs/05-conventions-api.md} §9, D-U).
     */
    @Transactional(readOnly = true)
    public Project execute(String slug) {
        return projectRepository.findPublishedBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException(
                ErrorCode.RESOURCE_NOT_FOUND, "Projet introuvable."));
    }
}
