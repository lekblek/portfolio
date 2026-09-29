package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crée une technologie (D-CW, comme D-CS). Nom déjà pris, sans tenir compte de la casse : 409
 * {@code NAME_ALREADY_USED} ; slug pris : suffixé dans la longueur de la colonne (D-BD).
 */
@Service
@RequiredArgsConstructor
public class CreateTechnologyUseCase {

    private final TechnologyRepository technologyRepository;

    @Transactional
    public Technology execute(TechnologyDraft draft) {
        if (technologyRepository.existsByName(draft.name(), null)) {
            throw new BusinessRuleViolationException(ErrorCode.NAME_ALREADY_USED, "Ce nom est déjà utilisé.");
        }
        Slug wanted = draft.slug() != null ? Slug.of(draft.slug()) : Slug.fromText(draft.name(), Technology.SLUG_MAX_LENGTH);
        Slug free = wanted.firstAvailable(candidate -> technologyRepository.existsBySlug(candidate, null),
            Technology.SLUG_MAX_LENGTH);
        return technologyRepository.create(new Technology(null, draft.name(), free, draft.displayOrder()));
    }
}
