package com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity.TechnologyEntity;
import org.springframework.data.repository.Repository;

/**
 * Accès Spring Data aux technologies. Seules les méthodes utilisées par les adaptateurs sont déclarées.
 */
public interface TechnologyJpaRepository extends Repository<TechnologyEntity, Long> {

    TechnologyEntity save(TechnologyEntity entity);

    /**
     * Référence sans requête, pour associer une technologie existante à un projet.
     */
    TechnologyEntity getReferenceById(Long id);
}
