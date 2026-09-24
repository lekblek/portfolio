package com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectFilter;
import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity.ProjectEntity;
import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity.TechnologyEntity;
import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.mapper.ProjectPersistenceMapper;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.scalke.portfolio.backend.project.domain.model.ProjectVisibility.PUBLISHED;

/**
 * Adaptateur JPA du port {@link ProjectRepository}.
 * <p>
 * La règle « seul {@code PUBLISHED} est public » (invariant 11, D-U) est appliquée dans les requêtes,
 * pour que la pagination compte uniquement les projets visibles. Le mapping en records a lieu dans la
 * transaction : les technologies d'une page y sont chargées en une requête (D-AB). Chaque méthode est
 * transactionnelle pour rester correcte hors cas d'usage (seed de développement) ; appelée depuis un
 * cas d'usage, elle rejoint sa transaction.
 */
@Repository
@RequiredArgsConstructor
public class ProjectRepositoryAdapter implements ProjectRepository {

    /**
     * Ordre d'affichage public, total grâce à l'identifiant en dernier critère (même principe que D-L).
     */
    private static final Sort PUBLIC_ORDER = Sort.by(
        Sort.Order.asc("displayOrder"),
        Sort.Order.desc("startDate"),
        Sort.Order.asc("id"));

    private final ProjectJpaRepository repository;
    private final TechnologyJpaRepository technologyRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<Project> findPublished(ProjectFilter filter, PageQuery query) {
        Pageable pageable = PageRequest.of(query.page(), query.size(), PUBLIC_ORDER);
        Page<ProjectEntity> entities = filter.hasTechnology()
            ? repository.findByVisibilityAndTechnologiesSlug(PUBLISHED, filter.technologySlug(), pageable)
            : repository.findByVisibility(PUBLISHED, pageable);
        Page<Project> page = entities.map(ProjectPersistenceMapper::toDomain);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findPublishedBySlug(String slug) {
        return repository.findBySlugAndVisibility(slug, PUBLISHED)
            .map(ProjectPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAny() {
        return repository.count() > 0;
    }

    @Override
    @Transactional
    public Project create(Project project) {
        if (project.id() != null) {
            throw new IllegalArgumentException("create expects a new project, got id " + project.id());
        }
        List<TechnologyEntity> technologies = project.technologies().stream()
            .map(this::existingTechnology)
            .toList();
        ProjectEntity saved = repository.save(ProjectPersistenceMapper.toNewEntity(project, technologies));
        return ProjectPersistenceMapper.toDomain(saved);
    }

    private TechnologyEntity existingTechnology(Technology technology) {
        if (technology.id() == null) {
            throw new IllegalArgumentException(
                "technology " + technology.slug() + " must exist before being linked to a project");
        }
        return technologyRepository.getReferenceById(technology.id());
    }
}
