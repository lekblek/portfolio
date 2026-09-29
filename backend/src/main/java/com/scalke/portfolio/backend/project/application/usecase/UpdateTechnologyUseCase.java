package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remplace le nom, le slug et l'ordre d'affichage d'une technologie (D-CW). Un terme n'est pas publié : son slug reste
 * modifiable (D-BC) ; sans slug saisi, il est conservé.
 */
@Service
@RequiredArgsConstructor
public class UpdateTechnologyUseCase {

    private final TechnologyRepository technologyRepository;

    @Transactional
    public Technology execute(Long id, TechnologyDraft draft) {
        Technology current = technologyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Technologie introuvable."));
        if (technologyRepository.existsByName(draft.name(), id)) {
            throw new BusinessRuleViolationException(ErrorCode.NAME_ALREADY_USED, "Ce nom est déjà utilisé.");
        }
        Slug wanted = draft.slug() != null ? Slug.of(draft.slug()) : current.slug();
        Slug free = wanted.firstAvailable(candidate -> technologyRepository.existsBySlug(candidate, id),
            Technology.SLUG_MAX_LENGTH);
        return technologyRepository.update(new Technology(id, draft.name(), free, draft.displayOrder()));
    }
}
