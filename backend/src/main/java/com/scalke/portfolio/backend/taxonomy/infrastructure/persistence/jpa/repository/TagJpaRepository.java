package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.entity.TagEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagJpaRepository extends Repository<TagEntity, Long> {

    Optional<TagEntity> findBySlug(String slug);

    List<TagEntity> findByIdIn(Collection<Long> ids);

    @Query("select t from TagEntity t order by lower(t.name), t.slug")
    List<TagEntity> findAllByName();

    Optional<TagEntity> findById(Long id);

    @Query("select count(t) > 0 from TagEntity t where t.slug = :slug and (:excludedId is null or t.id <> :excludedId)")
    boolean existsBySlug(String slug, Long excludedId);

    @Query("select count(t) > 0 from TagEntity t where lower(t.name) = lower(:name) and (:excludedId is null or t.id <> :excludedId)")
    boolean existsByName(String name, Long excludedId);

    TagEntity saveAndFlush(TagEntity entity);

    void deleteById(Long id);

    void flush();
}
