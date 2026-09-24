package com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Accès Spring Data aux projets. Seules les méthodes utilisées par l'adaptateur sont déclarées.
 */
public interface ProjectJpaRepository extends Repository<ProjectEntity, Long> {

    Page<ProjectEntity> findByVisibility(ProjectVisibility visibility, Pageable pageable);

    Optional<ProjectEntity> findBySlugAndVisibility(String slug, ProjectVisibility visibility);

    long count();

    ProjectEntity save(ProjectEntity entity);
}
