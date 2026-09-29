package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.media.application.query.MediaQueryService;
import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectContent;
import com.scalke.portfolio.backend.project.domain.model.ProjectScreenshot;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.InvalidInputException;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Règles communes aux cas d'usage d'administration des projets (D-CX) : transforme une saisie en contenu valide,
 * chaque refus visant son champ (400 {@code VALIDATION_FAILED}) plutôt qu'une erreur du domaine ou de PostgreSQL.
 */
@Component
@RequiredArgsConstructor
class ProjectAdministration {

    private final ProjectRepository projectRepository;
    private final TechnologyRepository technologyRepository;
    private final MediaQueryService media;

    Project find(Long id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Projet introuvable."));
    }

    /**
     * Période cohérente (invariants 17 et 21), technologies existantes, couverture et captures parmi les images du
     * catalogue (D-BV), chaque capture au plus une fois (invariant 27), rangée dans l'ordre saisi.
     */
    ProjectContent resolve(ProjectDraft draft) {
        if (draft.endDate() != null && draft.endDate().isBefore(draft.startDate())) {
            throw new InvalidInputException("endDate", "La date de fin précède la date de début.");
        }
        if ((draft.stage() == ProjectStage.IN_PROGRESS) != (draft.endDate() == null)) {
            throw new InvalidInputException("stage",
                "Un projet en cours n'a pas de date de fin ; un projet terminé en a une.");
        }
        List<Technology> technologies = technologyRepository.findAllById(draft.technologyIds());
        if (technologies.size() != draft.technologyIds().size()) {
            throw new InvalidInputException("technologyIds", "Technologie inconnue.");
        }
        return new ProjectContent(draft.title(), draft.shortDescription(), draft.descriptionMarkdown(), draft.stage(),
            draft.visibility(), new DateRange(draft.startDate(), draft.endDate()), draft.repositoryUrl(),
            draft.demoUrl(), draft.featured(), draft.displayOrder(), technologies, cover(draft.coverMediaId()),
            screenshots(draft.screenshots()));
    }

    private Long cover(Long coverMediaId) {
        if (coverMediaId != null && !media.imagesById(Set.of(coverMediaId)).containsKey(coverMediaId)) {
            throw new InvalidInputException("coverMediaId", "Image de couverture inconnue.");
        }
        return coverMediaId;
    }

    private List<ProjectScreenshot> screenshots(List<ProjectDraft.Screenshot> drafts) {
        List<Long> ids = drafts.stream().map(ProjectDraft.Screenshot::mediaId).toList();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new InvalidInputException("screenshots", "Une capture figure deux fois.");
        }
        Map<Long, ?> images = media.imagesById(ids);
        if (!images.keySet().containsAll(ids)) {
            throw new InvalidInputException("screenshots", "Capture inconnue.");
        }
        List<ProjectScreenshot> screenshots = new ArrayList<>();
        for (ProjectDraft.Screenshot draft : drafts) {
            screenshots.add(new ProjectScreenshot(draft.mediaId(), draft.caption(), screenshots.size()));
        }
        return screenshots;
    }
}
