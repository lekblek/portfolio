package com.scalke.portfolio.backend.media.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.media.infrastructure.persistence.jpa.entity.MediaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Accès Spring Data au catalogue. Seules les méthodes utilisées par l'adaptateur sont déclarées.
 */
public interface MediaJpaRepository extends Repository<MediaEntity, Long> {

    MediaEntity save(MediaEntity entity);

    Optional<MediaEntity> findById(Long id);

    Optional<MediaEntity> findFirstByOriginalNameOrderByIdAsc(String originalName);

    List<MediaEntity> findAllById(Iterable<Long> ids);

    Page<MediaEntity> findAll(Pageable pageable);

    void deleteById(Long id);

    void flush();
}
