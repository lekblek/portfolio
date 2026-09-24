package com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.entity.PublicationEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Accès Spring Data aux publications. Les lectures publiques passent par des critères combinables
 * ({@link PublicationSpecifications}, D-AR) ; seules les autres méthodes utilisées sont déclarées.
 */
public interface PublicationJpaRepository
    extends Repository<PublicationEntity, Long>, JpaSpecificationExecutor<PublicationEntity> {

    long count();

    Optional<PublicationEntity> findById(Long id);

    PublicationEntity save(PublicationEntity entity);
}
