package com.scalke.portfolio.backend.publication.web.dto;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationContent;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.shared.api.InputPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Création d'une publication (D-CU) : le type, fixé pour toujours, et la saisie de {@link UpdatePublicationRequest}.
 * {@code slug} facultatif : généré depuis le titre. {@code featured} absent → {@code false} (un {@code boolean} absent
 * serait refusé par Jackson 3).
 */
public record CreatePublicationRequest(
    @NotNull PublicationType type,
    @NotBlank @Size(max = Publication.TITLE_MAX_LENGTH)
    @Pattern(regexp = InputPatterns.HAS_LETTER_OR_DIGIT, message = InputPatterns.HAS_LETTER_OR_DIGIT_MESSAGE)
    String title,
    @Size(max = Publication.TITLE_MAX_LENGTH) @Pattern(regexp = InputPatterns.SLUG, message = InputPatterns.SLUG_MESSAGE)
    String slug,
    @NotBlank @Size(max = Publication.SUMMARY_MAX_LENGTH) String summary,
    @NotNull @Size(max = Publication.CONTENT_MAX_LENGTH) String contentMarkdown,
    Boolean featured,
    Long categoryId,
    Set<@NotNull Long> tagIds,
    Long coverMediaId,
    @Size(max = Publication.SEO_TITLE_MAX_LENGTH) String seoTitle,
    @Size(max = Publication.SEO_DESCRIPTION_MAX_LENGTH) String seoDescription
) {

    public PublicationContent toContent() {
        return new PublicationContent(title, summary, contentMarkdown, Boolean.TRUE.equals(featured), categoryId,
            tagIds, coverMediaId, seoTitle, seoDescription);
    }
}
