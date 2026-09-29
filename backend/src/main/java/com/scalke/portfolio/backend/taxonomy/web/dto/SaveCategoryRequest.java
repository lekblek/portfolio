package com.scalke.portfolio.backend.taxonomy.web.dto;

import com.scalke.portfolio.backend.taxonomy.application.usecase.TermDraft;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Création ou remplacement d'une catégorie (D-CS). {@code slug} facultatif : généré depuis le nom à la création,
 * conservé à la modification.
 */
public record SaveCategoryRequest(
    @NotBlank @Size(max = Category.NAME_MAX_LENGTH)
    @Pattern(regexp = TermRequests.HAS_LETTER_OR_DIGIT, message = TermRequests.HAS_LETTER_OR_DIGIT_MESSAGE)
    String name,
    @Size(max = Category.SLUG_MAX_LENGTH) @Pattern(regexp = TermRequests.SLUG, message = TermRequests.SLUG_MESSAGE)
    String slug,
    @Size(max = Category.DESCRIPTION_MAX_LENGTH) String description
) {

    public TermDraft toDraft() {
        return new TermDraft(name, slug, description);
    }
}
