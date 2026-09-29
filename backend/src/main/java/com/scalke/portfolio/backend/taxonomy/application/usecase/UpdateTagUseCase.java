package com.scalke.portfolio.backend.taxonomy.application.usecase;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.domain.port.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remplace le nom, le slug d'un tag (D-CS). Un terme n'est pas publié : son slug reste
 * modifiable (D-BC) ; sans slug saisi, il est conservé.
 */
@Service
@RequiredArgsConstructor
public class UpdateTagUseCase {

    private final TagRepository tagRepository;

    @Transactional
    public Tag execute(Long id, TermDraft draft) {
        Tag current = tagRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Tag introuvable."));
        if (tagRepository.existsByName(draft.name(), id)) {
            throw new BusinessRuleViolationException(ErrorCode.NAME_ALREADY_USED, "Ce nom est déjà utilisé.");
        }
        var slug = TermSlugs.choose(draft, current.slug(), Tag.SLUG_MAX_LENGTH,
            candidate -> tagRepository.existsBySlug(candidate, id));
        return tagRepository.update(new Tag(id, draft.name(), slug));
    }
}
