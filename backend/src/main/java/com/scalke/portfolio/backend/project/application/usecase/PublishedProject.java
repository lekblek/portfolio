package com.scalke.portfolio.backend.project.application.usecase;

import com.scalke.portfolio.backend.media.application.query.PublicImage;
import com.scalke.portfolio.backend.project.domain.model.Project;

import java.util.List;

/**
 * Projet publié et ses images publiques (D-BW) : couverture ({@code null} si aucune) et captures dans leur
 * ordre d'affichage. Dans une liste, seule la couverture est assemblée ({@code screenshots} vide).
 */
public record PublishedProject(Project project, PublicImage cover, List<PublishedScreenshot> screenshots) {

    public PublishedProject {
        screenshots = List.copyOf(screenshots);
    }
}
