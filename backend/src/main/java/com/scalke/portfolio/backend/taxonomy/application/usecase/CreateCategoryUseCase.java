package com.scalke.portfolio.backend.taxonomy.application.usecase;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.port.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crée une catégorie (D-CS). Nom déjà pris, sans tenir compte de la casse : 409 {@code NAME_ALREADY_USED} ; slug
 * pris : suffixé (D-BD).
 */
@Service
@RequiredArgsConstructor
public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category execute(TermDraft draft) {
        if (categoryRepository.existsByName(draft.name(), null)) {
            throw new BusinessRuleViolationException(ErrorCode.NAME_ALREADY_USED, "Ce nom est déjà utilisé.");
        }
        var slug = TermSlugs.choose(draft, null, Category.SLUG_MAX_LENGTH,
            candidate -> categoryRepository.existsBySlug(candidate, null));
        return categoryRepository.create(new Category(null, draft.name(), slug, draft.description()));
    }
}
