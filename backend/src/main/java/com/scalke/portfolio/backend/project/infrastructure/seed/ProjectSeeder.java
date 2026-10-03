package com.scalke.portfolio.backend.project.infrastructure.seed;

import com.scalke.portfolio.backend.media.application.query.MediaQueryService;
import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.project.application.usecase.CreateProjectUseCase;
import com.scalke.portfolio.backend.project.application.usecase.ProjectDraft;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Technologies et projets de démonstration du profil {@code dev}, décrits par {@code dev-seed/projects.json} (D-EU) :
 * assez de projets pour franchir deux pages de la liste publique (10 par page) et de l'administration (20), dans
 * tous les états (brouillon, publié, archivé ; en cours, terminé ; mis en avant ou non ; avec ou sans couverture,
 * captures et adresses). Ce ne sont pas des données réelles (voir docs/01-perimetre-v1.md §20).
 * <p>
 * Additif et idempotent : une technologie ou un projet n'est créé que si son slug manque ; rien d'existant n'est
 * modifié, une saisie manuelle est donc préservée. Les projets passent par le cas d'usage de création (mêmes
 * vérifications que l'administration) ; leurs images sont les médias de démonstration envoyés par le module
 * {@code media} (retrouvés par leur nom, absents si les fichiers manquent).
 */
@Component
@Profile("dev")
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ProjectSeeder implements ApplicationRunner {

    static final String SOURCE = "dev-seed/projects.json";

    private final ProjectRepository projectRepository;
    private final TechnologyRepository technologyRepository;
    private final CreateProjectUseCase createProjectUseCase;
    private final MediaQueryService media;
    private final JsonMapper jsonMapper;

    @Override
    public void run(ApplicationArguments args) {
        ClassPathResource source = new ClassPathResource(SOURCE);
        if (!source.exists()) {
            return;
        }
        DemoProjects demo = read(source);
        Map<String, Technology> technologies = technologies(demo.technologies());
        int created = 0;
        for (DemoProject project : demo.projects()) {
            if (!projectRepository.existsBySlug(Slug.fromText(project.title()), null)) {
                createProjectUseCase.execute(null, draft(project, technologies));
                created++;
            }
        }
        if (created > 0) {
            log.info("{} projets de démonstration créés (profil dev)", created);
        }
    }

    /**
     * Technologies manquantes créées dans l'ordre du fichier ; toutes renvoyées par nom.
     */
    private Map<String, Technology> technologies(List<String> names) {
        Map<String, Technology> existing = technologyRepository.findAll().stream()
            .collect(Collectors.toMap(technology -> technology.slug().value(), Function.identity()));
        for (int order = 0; order < names.size(); order++) {
            Slug slug = Slug.fromText(names.get(order));
            if (!existing.containsKey(slug.value())) {
                existing.put(slug.value(), technologyRepository.create(new Technology(null, names.get(order), slug, order)));
            }
        }
        return names.stream().collect(Collectors.toMap(Function.identity(),
            name -> existing.get(Slug.fromText(name).value())));
    }

    private ProjectDraft draft(DemoProject project, Map<String, Technology> technologies) {
        Set<Long> technologyIds = project.technologies().stream()
            .map(name -> technologies.get(name).id())
            .collect(Collectors.toSet());
        List<ProjectDraft.Screenshot> screenshots = project.screenshots().stream()
            .flatMap(screenshot -> mediaId(screenshot.file()).stream()
                .map(id -> new ProjectDraft.Screenshot(id, screenshot.caption())))
            .toList();
        return new ProjectDraft(project.title(), project.shortDescription(), project.descriptionMarkdown(),
            project.stage(), project.visibility(), project.startDate(), project.endDate(), project.repositoryUrl(),
            project.demoUrl(), project.featured(), project.displayOrder(), technologyIds,
            project.cover() == null ? null : mediaId(project.cover()).orElse(null), screenshots);
    }

    private Optional<Long> mediaId(String file) {
        return media.findByOriginalName(file).map(Media::id);
    }

    private DemoProjects read(ClassPathResource source) {
        try (InputStream content = source.getInputStream()) {
            return jsonMapper.readValue(content, DemoProjects.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    record DemoProjects(List<String> technologies, List<DemoProject> projects) {
    }

    record DemoProject(String title, String shortDescription, String descriptionMarkdown, ProjectStage stage,
                       ProjectVisibility visibility, LocalDate startDate, LocalDate endDate, String repositoryUrl,
                       String demoUrl, boolean featured, int displayOrder, List<String> technologies, String cover,
                       List<DemoScreenshot> screenshots) {
    }

    record DemoScreenshot(String file, String caption) {
    }
}
