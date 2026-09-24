package com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.entity.PublicationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Accès Spring Data aux publications. Seules les méthodes utilisées par l'adaptateur sont déclarées.
 */
public interface PublicationJpaRepository extends Repository<PublicationEntity, Long> {

    /**
     * Règle de visibilité publique (invariants 7 à 10, D-AH), écrite une seule fois et partagée par
     * toutes les requêtes publiques. Le tri et la pagination sont fournis par le {@code Pageable}.
     */
    String VISIBLE_AT_NOW = """
        (p.status = com.scalke.portfolio.backend.publication.domain.model.PublicationStatus.PUBLISHED
         or (p.status = com.scalke.portfolio.backend.publication.domain.model.PublicationStatus.SCHEDULED
             and p.publishedAt <= :now))
        """;

    @Query("select p from PublicationEntity p where " + VISIBLE_AT_NOW)
    Page<PublicationEntity> findVisible(@Param("now") Instant now, Pageable pageable);

    @Query("select p from PublicationEntity p where p.type = :type and " + VISIBLE_AT_NOW)
    Page<PublicationEntity> findVisibleByType(
        @Param("type") PublicationType type, @Param("now") Instant now, Pageable pageable);

    @Query("select p from PublicationEntity p where p.slug = :slug and " + VISIBLE_AT_NOW)
    Optional<PublicationEntity> findVisibleBySlug(@Param("slug") String slug, @Param("now") Instant now);

    long count();

    PublicationEntity save(PublicationEntity entity);
}
