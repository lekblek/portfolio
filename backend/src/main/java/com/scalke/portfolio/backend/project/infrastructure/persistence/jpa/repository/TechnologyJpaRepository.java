package com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity.TechnologyEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Accès Spring Data aux technologies. Seules les méthodes utilisées par les adaptateurs sont déclarées.
 */
public interface TechnologyJpaRepository extends Repository<TechnologyEntity, Long> {

    @Query("select t from TechnologyEntity t order by t.displayOrder, lower(t.name), t.slug")
    List<TechnologyEntity> findAllInDisplayOrder();

    Optional<TechnologyEntity> findById(Long id);

    List<TechnologyEntity> findByIdIn(Collection<Long> ids);

    @Query("select count(t) > 0 from TechnologyEntity t where t.slug = :slug and (:excludedId is null or t.id <> :excludedId)")
    boolean existsBySlug(String slug, Long excludedId);

    @Query("select count(t) > 0 from TechnologyEntity t where lower(t.name) = lower(:name) and (:excludedId is null or t.id <> :excludedId)")
    boolean existsByName(String name, Long excludedId);

    TechnologyEntity saveAndFlush(TechnologyEntity entity);

    void deleteById(Long id);

    void flush();

    /**
     * Référence sans requête, pour associer une technologie existante à un projet.
     */
    TechnologyEntity getReferenceById(Long id);
}
