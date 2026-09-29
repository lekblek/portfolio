package com.scalke.portfolio.backend.taxonomy.application.usecase;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.port.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remplace le nom, le slug et la description d'une catégorie (D-CS). Un terme n'est pas publié : son slug reste
 * modifiable (D-BC) ; sans slug saisi, il est conservé.
 */
@Service
@RequiredArgsConstructor
public class UpdateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category execute(Long id, TermDraft draft) {
        Category current = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Catégorie introuvable."));
        if (categoryRepository.existsByName(draft.name(), id)) {
            throw new BusinessRuleViolationException(ErrorCode.NAME_ALREADY_USED, "Ce nom est déjà utilisé.");
        }
        var slug = TermSlugs.choose(draft, current.slug(), Category.SLUG_MAX_LENGTH,
            candidate -> categoryRepository.existsBySlug(candidate, id));
        return categoryRepository.update(new Category(id, draft.name(), slug, draft.description()));
    }
}
