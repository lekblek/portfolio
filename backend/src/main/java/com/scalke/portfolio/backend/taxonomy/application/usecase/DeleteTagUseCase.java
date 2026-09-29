package com.scalke.portfolio.backend.taxonomy.application.usecase;

import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.taxonomy.domain.port.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supprime un tag qu'aucune publication n'utilise (invariant 25) : PostgreSQL refuse sinon, 409
 * {@code TERM_STILL_USED} (D-CS).
 */
@Service
@RequiredArgsConstructor
public class DeleteTagUseCase {

    private final TagRepository tagRepository;

    @Transactional
    public void execute(Long id) {
        if (tagRepository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Tag introuvable.");
        }
        tagRepository.delete(id);
    }
}
