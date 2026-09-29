package com.scalke.portfolio.backend.project.web.dto;

import com.scalke.portfolio.backend.project.application.usecase.TechnologyDraft;
import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.shared.api.InputPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Création ou remplacement d'une technologie (D-CW). {@code slug} facultatif : généré depuis le nom à la création,
 * conservé à la modification ; {@code displayOrder} absent → 0.
 */
public record SaveTechnologyRequest(
    @NotBlank @Size(max = Technology.NAME_MAX_LENGTH)
    @Pattern(regexp = InputPatterns.HAS_LETTER_OR_DIGIT, message = InputPatterns.HAS_LETTER_OR_DIGIT_MESSAGE)
    String name,
    @Size(max = Technology.SLUG_MAX_LENGTH) @Pattern(regexp = InputPatterns.SLUG, message = InputPatterns.SLUG_MESSAGE)
    String slug,
    @PositiveOrZero Integer displayOrder
) {

    public TechnologyDraft toDraft() {
        return new TechnologyDraft(name, slug, displayOrder == null ? 0 : displayOrder);
    }
}
