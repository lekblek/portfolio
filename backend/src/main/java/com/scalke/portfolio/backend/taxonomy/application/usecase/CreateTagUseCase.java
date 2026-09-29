package com.scalke.portfolio.backend.taxonomy.application.usecase;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.domain.port.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crée un tag (D-CS). Nom déjà pris, sans tenir compte de la casse : 409 {@code NAME_ALREADY_USED} ; slug
 * pris : suffixé (D-BD).
 */
@Service
@RequiredArgsConstructor
public class CreateTagUseCase {

    private final TagRepository tagRepository;

    @Transactional
    public Tag execute(TermDraft draft) {
        if (tagRepository.existsByName(draft.name(), null)) {
            throw new BusinessRuleViolationException(ErrorCode.NAME_ALREADY_USED, "Ce nom est déjà utilisé.");
        }
        var slug = TermSlugs.choose(draft, null, Tag.SLUG_MAX_LENGTH,
            candidate -> tagRepository.existsBySlug(candidate, null));
        return tagRepository.create(new Tag(null, draft.name(), slug));
    }
}
