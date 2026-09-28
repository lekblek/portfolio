package com.scalke.portfolio.backend.media.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.media.infrastructure.persistence.jpa.entity.MediaEntity;
import org.springframework.data.repository.Repository;

/**
 * Accès Spring Data au catalogue. Seules les méthodes utilisées par l'adaptateur sont déclarées.
 */
public interface MediaJpaRepository extends Repository<MediaEntity, Long> {

    MediaEntity save(MediaEntity entity);
}
