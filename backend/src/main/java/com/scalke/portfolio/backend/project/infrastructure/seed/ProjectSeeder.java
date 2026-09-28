package com.scalke.portfolio.backend.project.infrastructure.seed;

import com.scalke.portfolio.backend.media.application.usecase.MediaUpload;
import com.scalke.portfolio.backend.media.application.usecase.UploadMediaUseCase;
import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectScreenshot;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Technologies et projets de démonstration du profil `dev`, créés uniquement si la base ne contient
 * aucun projet. Ce ne sont pas des données réelles (voir docs/01-perimetre-v1.md §20). Le brouillon
 * permet de vérifier à la main qu'un projet non publié reste invisible.
 * <p>
 * Transactionnel : le vocabulaire, les images et les projets sont créés ensemble ou pas du tout, pour qu'un
 * redémarrage ne tente jamais de recréer des technologies déjà présentes. Les images de démonstration
 * (couverture et capture du premier projet) sont générées à la volée et envoyées par le cas d'usage du
 * module {@code media} : aucun fichier binaire dans le dépôt.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class ProjectSeeder implements ApplicationRunner {

    private final ProjectRepository projectRepository;
    private final TechnologyRepository technologyRepository;
    private final UploadMediaUseCase uploadMediaUseCase;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (projectRepository.existsAny()) {
            return;
        }
        Technology java = technologyRepository.create(technology("Java", 0));
        Technology springBoot = technologyRepository.create(technology("Spring Boot", 1));
        Technology angular = technologyRepository.create(technology("Angular", 2));
        Technology postgresql = technologyRepository.create(technology("PostgreSQL", 3));
        Technology docker = technologyRepository.create(technology("Docker", 4));
        Media cover = demoImage("portfolio-couverture.png", "Couverture de démonstration du portfolio",
            1200, 630, new Color(0x1E3A5F));
        Media screenshot = demoImage("portfolio-liste-des-projets.png", "Capture de démonstration : liste des projets",
            1280, 800, new Color(0x2E7D32));

        List.of(
            new Project(null, "Portfolio full-stack", Slug.fromText("Portfolio full-stack"),
                "Portfolio professionnel : Spring Boot, Angular SSR et PostgreSQL.",
                "## Objectif\n\nProjet de démonstration.",
                ProjectStage.IN_PROGRESS, ProjectVisibility.PUBLISHED,
                DateRange.ongoingSince(LocalDate.of(2026, 9, 1)),
                "https://example.test/portfolio", null, true, 0,
                List.of(java, springBoot, angular, postgresql, docker),
                cover.id(),
                List.of(new ProjectScreenshot(screenshot.id(), "Liste des projets", 0))),
            new Project(null, "Projet terminé de démonstration", Slug.fromText("Projet terminé de démonstration"),
                "Projet de démonstration terminé.",
                "## Bilan\n\nProjet de démonstration.",
                ProjectStage.COMPLETED, ProjectVisibility.PUBLISHED,
                DateRange.between(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)),
                null, "https://example.test/demo", false, 1,
                List.of(java, postgresql),
                null,
                List.of()),
            new Project(null, "Brouillon de démonstration", Slug.fromText("Brouillon de démonstration"),
                "Projet non publié : absent de l'API publique.",
                "Brouillon.",
                ProjectStage.IN_PROGRESS, ProjectVisibility.DRAFT,
                DateRange.ongoingSince(LocalDate.of(2026, 1, 1)),
                null, null, false, 2,
                List.of(angular),
                null,
                List.of())
        ).forEach(projectRepository::create);
        log.info("Technologies et projets de démonstration créés (profil dev)");
    }

    /**
     * Image PNG unie, générée en mémoire puis envoyée comme le ferait l'administration.
     */
    private Media demoImage(String name, String altText, int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", png);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return uploadMediaUseCase.execute(new MediaUpload(name, altText, new ByteArrayInputStream(png.toByteArray())));
    }

    /**
     * Slugs générés depuis les noms et les titres, comme le fera l'administration (D-BA).
     */
    private static Technology technology(String name, int displayOrder) {
        return new Technology(null, name, Slug.fromText(name), displayOrder);
    }
}
