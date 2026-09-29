package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.project.domain.model.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Un projet, quelle que soit sa visibilité, pour l'administration (D-CX).
 */
@Service
@RequiredArgsConstructor
public class GetProjectUseCase {

    private final ProjectAdministration administration;

    @Transactional(readOnly = true)
    public Project execute(Long id) {
        return administration.find(id);
    }
}
