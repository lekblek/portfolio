package com.scalke.portfolio.backend.project.web.dto;

import com.scalke.portfolio.backend.project.application.usecase.ProjectDraft;
import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectScreenshot;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.shared.api.InputPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Création ou remplacement d'un projet (D-CX), visibilité comprise. {@code slug} facultatif : généré depuis le titre à
 * la création, conservé à la modification. {@code screenshots} dans l'ordre d'affichage ; absents, champs facultatifs
 * vides : aucun ; {@code featured} absent → {@code false}, {@code displayOrder} absent → 0.
 */
public record SaveProjectRequest(
    @NotBlank @Size(max = Project.TITLE_MAX_LENGTH)
    @Pattern(regexp = InputPatterns.HAS_LETTER_OR_DIGIT, message = InputPatterns.HAS_LETTER_OR_DIGIT_MESSAGE)
    String title,
    @Size(max = Project.TITLE_MAX_LENGTH) @Pattern(regexp = InputPatterns.SLUG, message = InputPatterns.SLUG_MESSAGE)
    String slug,
    @NotBlank @Size(max = Project.SHORT_DESCRIPTION_MAX_LENGTH) String shortDescription,
    @NotNull @Size(max = Project.DESCRIPTION_MAX_LENGTH) String descriptionMarkdown,
    @NotNull ProjectStage stage,
    @NotNull ProjectVisibility visibility,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    @Size(max = Project.URL_MAX_LENGTH)
    @Pattern(regexp = InputPatterns.HTTP_URL, message = InputPatterns.HTTP_URL_MESSAGE)
    String repositoryUrl,
    @Size(max = Project.URL_MAX_LENGTH)
    @Pattern(regexp = InputPatterns.HTTP_URL, message = InputPatterns.HTTP_URL_MESSAGE)
    String demoUrl,
    Boolean featured,
    @PositiveOrZero Integer displayOrder,
    Set<@NotNull Long> technologyIds,
    Long coverMediaId,
    List<@NotNull @Valid Screenshot> screenshots
) {

    public ProjectDraft toDraft() {
        return new ProjectDraft(title, shortDescription, descriptionMarkdown, stage, visibility, startDate, endDate,
            repositoryUrl, demoUrl, Boolean.TRUE.equals(featured), displayOrder == null ? 0 : displayOrder,
            technologyIds, coverMediaId,
            screenshots == null ? null : screenshots.stream().map(Screenshot::toDraft).toList());
    }

    public record Screenshot(@NotNull Long mediaId, @Size(max = ProjectScreenshot.MAX_CAPTION_LENGTH) String caption) {

        ProjectDraft.Screenshot toDraft() {
            return new ProjectDraft.Screenshot(mediaId, caption);
        }
    }
}
