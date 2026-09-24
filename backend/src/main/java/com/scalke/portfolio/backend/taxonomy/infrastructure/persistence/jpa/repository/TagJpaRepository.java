package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.entity.TagEntity;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagJpaRepository extends Repository<TagEntity, Long> {

    Optional<TagEntity> findBySlug(String slug);

    List<TagEntity> findByIdIn(Collection<Long> ids);

    TagEntity save(TagEntity entity);
}
