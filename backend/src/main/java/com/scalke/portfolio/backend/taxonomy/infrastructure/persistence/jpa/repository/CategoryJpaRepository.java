package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.entity.CategoryEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryJpaRepository extends Repository<CategoryEntity, Long> {

    Optional<CategoryEntity> findBySlug(String slug);

    List<CategoryEntity> findByIdIn(Collection<Long> ids);

    @Query("select t from CategoryEntity t order by lower(t.name), t.slug")
    List<CategoryEntity> findAllByName();

    Optional<CategoryEntity> findById(Long id);

    @Query("select count(t) > 0 from CategoryEntity t where t.slug = :slug and (:excludedId is null or t.id <> :excludedId)")
    boolean existsBySlug(String slug, Long excludedId);

    @Query("select count(t) > 0 from CategoryEntity t where lower(t.name) = lower(:name) and (:excludedId is null or t.id <> :excludedId)")
    boolean existsByName(String name, Long excludedId);

    CategoryEntity saveAndFlush(CategoryEntity entity);

    void deleteById(Long id);

    void flush();
}
