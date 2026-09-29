package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supprime une technologie qu'aucun projet n'utilise (invariant 22) ; sinon 409 {@code TERM_STILL_USED} (D-CW).
 */
@Service
@RequiredArgsConstructor
public class DeleteTechnologyUseCase {

    private final TechnologyRepository technologyRepository;

    @Transactional
    public void execute(Long id) {
        if (technologyRepository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Technologie introuvable.");
        }
        technologyRepository.delete(id);
    }
}
