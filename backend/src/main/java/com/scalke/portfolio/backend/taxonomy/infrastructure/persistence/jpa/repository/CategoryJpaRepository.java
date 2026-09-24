package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.entity.CategoryEntity;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryJpaRepository extends Repository<CategoryEntity, Long> {

    Optional<CategoryEntity> findBySlug(String slug);

    List<CategoryEntity> findByIdIn(Collection<Long> ids);

    CategoryEntity save(CategoryEntity entity);
}
