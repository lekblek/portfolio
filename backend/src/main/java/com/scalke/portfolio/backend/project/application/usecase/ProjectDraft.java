package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Saisie d'un projet par l'administrateur (D-CX), avant résolution des références : technologies et médias par
 * identifiant, captures dans l'ordre d'affichage voulu. Espaces de début et de fin retirés ; adresses vides → aucune ;
 * technologies et captures absentes → aucune.
 */
public record ProjectDraft(
    String title,
    String shortDescription,
    String descriptionMarkdown,
    ProjectStage stage,
    ProjectVisibility visibility,
    LocalDate startDate,
    LocalDate endDate,
    String repositoryUrl,
    String demoUrl,
    boolean featured,
    int displayOrder,
    Set<Long> technologyIds,
    Long coverMediaId,
    List<Screenshot> screenshots
) {

    public ProjectDraft {
        title = title == null ? null : title.strip();
        shortDescription = shortDescription == null ? null : shortDescription.strip();
        repositoryUrl = blankToNull(repositoryUrl);
        demoUrl = blankToNull(demoUrl);
        technologyIds = technologyIds == null ? Set.of() : Set.copyOf(technologyIds);
        screenshots = screenshots == null ? List.of() : List.copyOf(screenshots);
    }

    /**
     * Capture saisie : image du catalogue et légende facultative (vide → aucune).
     */
    public record Screenshot(Long mediaId, String caption) {

        public Screenshot {
            caption = blankToNull(caption);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
