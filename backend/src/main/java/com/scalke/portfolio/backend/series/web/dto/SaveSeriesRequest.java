package com.scalke.portfolio.backend.series.web.dto;

import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.model.SeriesContent;
import com.scalke.portfolio.backend.shared.api.InputPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Création ou remplacement d'une série, sans ses chapitres (D-CV). {@code slug} facultatif : généré depuis le titre à
 * la création, conservé à la modification.
 */
public record SaveSeriesRequest(
    @NotBlank @Size(max = Series.TITLE_MAX_LENGTH)
    @Pattern(regexp = InputPatterns.HAS_LETTER_OR_DIGIT, message = InputPatterns.HAS_LETTER_OR_DIGIT_MESSAGE)
    String title,
    @Size(max = Series.TITLE_MAX_LENGTH) @Pattern(regexp = InputPatterns.SLUG, message = InputPatterns.SLUG_MESSAGE)
    String slug,
    @NotNull @Size(max = Series.DESCRIPTION_MAX_LENGTH) String descriptionMarkdown,
    Long coverMediaId
) {

    public SeriesContent toContent() {
        return new SeriesContent(title, descriptionMarkdown, coverMediaId);
    }
}
