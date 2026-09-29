package com.scalke.portfolio.backend.taxonomy.web.dto;

import com.scalke.portfolio.backend.taxonomy.application.usecase.TermDraft;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Création ou remplacement d'un tag (D-CS). {@code slug} facultatif : généré depuis le nom à la création, conservé à
 * la modification.
 */
public record SaveTagRequest(
    @NotBlank @Size(max = Tag.NAME_MAX_LENGTH)
    @Pattern(regexp = TermRequests.HAS_LETTER_OR_DIGIT, message = TermRequests.HAS_LETTER_OR_DIGIT_MESSAGE)
    String name,
    @Size(max = Tag.SLUG_MAX_LENGTH) @Pattern(regexp = TermRequests.SLUG, message = TermRequests.SLUG_MESSAGE)
    String slug
) {

    public TermDraft toDraft() {
        return new TermDraft(name, slug, null);
    }
}
