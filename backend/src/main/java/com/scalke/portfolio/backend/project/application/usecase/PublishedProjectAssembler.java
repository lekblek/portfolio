package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.media.application.query.MediaQueryService;
import com.scalke.portfolio.backend.media.application.query.PublicImage;
import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectScreenshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Associe aux projets leurs images, par la façade du module {@code media} (ADR 0002, D-BW) : une requête pour
 * tout un lot, aucune si aucun projet n'a d'image. Une image absente du résultat (média qui n'est pas une
 * image) est ignorée. S'exécute dans la transaction du cas d'usage.
 */
@Component
@RequiredArgsConstructor
class PublishedProjectAssembler {

    private final MediaQueryService media;

    /**
     * Pour une liste : la couverture seulement.
     */
    List<PublishedProject> withCovers(Collection<Project> projects) {
        Map<Long, PublicImage> images = media.imagesById(projects.stream()
            .map(Project::coverMediaId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()));
        return projects.stream()
            .map(project -> new PublishedProject(project, cover(project, images), List.of()))
            .toList();
    }

    /**
     * Pour le détail : la couverture et les captures, dans leur ordre d'affichage.
     */
    PublishedProject withAllImages(Project project) {
        Set<Long> ids = Stream.concat(
                Stream.ofNullable(project.coverMediaId()),
                project.screenshots().stream().map(ProjectScreenshot::mediaId))
            .collect(Collectors.toSet());
        Map<Long, PublicImage> images = media.imagesById(ids);
        List<PublishedScreenshot> screenshots = project.screenshots().stream()
            .filter(screenshot -> images.containsKey(screenshot.mediaId()))
            .map(screenshot -> new PublishedScreenshot(images.get(screenshot.mediaId()), screenshot.caption()))
            .toList();
        return new PublishedProject(project, cover(project, images), screenshots);
    }

    private static PublicImage cover(Project project, Map<Long, PublicImage> images) {
        return project.coverMediaId() == null ? null : images.get(project.coverMediaId());
    }
}
