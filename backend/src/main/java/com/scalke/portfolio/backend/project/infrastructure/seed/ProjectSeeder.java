package com.scalke.portfolio.backend.project.infrastructure.seed;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Technologies et projets de démonstration du profil `dev`, créés uniquement si la base ne contient
 * aucun projet. Ce ne sont pas des données réelles (voir docs/01-perimetre-v1.md §20). Le brouillon
 * permet de vérifier à la main qu'un projet non publié reste invisible.
 * <p>
 * Transactionnel : le vocabulaire et les projets sont créés ensemble ou pas du tout, pour qu'un
 * redémarrage ne tente jamais de recréer des technologies déjà présentes.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class ProjectSeeder implements ApplicationRunner {

    private final ProjectRepository projectRepository;
    private final TechnologyRepository technologyRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (projectRepository.existsAny()) {
            return;
        }
        Technology java = technologyRepository.create(new Technology(null, "Java", "java", 0));
        Technology springBoot = technologyRepository.create(new Technology(null, "Spring Boot", "spring-boot", 1));
        Technology angular = technologyRepository.create(new Technology(null, "Angular", "angular", 2));
        Technology postgresql = technologyRepository.create(new Technology(null, "PostgreSQL", "postgresql", 3));
        Technology docker = technologyRepository.create(new Technology(null, "Docker", "docker", 4));

        List.of(
            new Project(null, "Portfolio full-stack", "portfolio-full-stack",
                "Portfolio professionnel : Spring Boot, Angular SSR et PostgreSQL.",
                "## Objectif\n\nProjet de démonstration.",
                ProjectStage.IN_PROGRESS, ProjectVisibility.PUBLISHED,
                DateRange.ongoingSince(LocalDate.of(2026, 9, 1)),
                "https://example.test/portfolio", null, true, 0,
                List.of(java, springBoot, angular, postgresql, docker)),
            new Project(null, "Projet terminé de démonstration", "projet-termine-de-demonstration",
                "Projet de démonstration terminé.",
                "## Bilan\n\nProjet de démonstration.",
                ProjectStage.COMPLETED, ProjectVisibility.PUBLISHED,
                DateRange.between(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)),
                null, "https://example.test/demo", false, 1,
                List.of(java, postgresql)),
            new Project(null, "Brouillon de démonstration", "brouillon-de-demonstration",
                "Projet non publié : absent de l'API publique.",
                "Brouillon.",
                ProjectStage.IN_PROGRESS, ProjectVisibility.DRAFT,
                DateRange.ongoingSince(LocalDate.of(2026, 1, 1)),
                null, null, false, 2,
                List.of(angular))
        ).forEach(projectRepository::create);
        log.info("Technologies et projets de démonstration créés (profil dev)");
    }
}
