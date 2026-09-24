package com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.publication.domain.model.PublicationFilter;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.entity.PublicationEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Set;

/**
 * Critères des requêtes publiques (D-AR). Chaque règle est écrite une seule fois ; les requêtes les
 * combinent selon le filtre demandé.
 */
final class PublicationSpecifications {

    private PublicationSpecifications() {
    }

    /**
     * Règle de visibilité publique (invariants 7 à 10, D-AH) : {@code PUBLISHED}, ou {@code SCHEDULED}
     * dont la date de publication est passée à {@code now}.
     */
    static Specification<PublicationEntity> visibleAt(Instant now) {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("status"), PublicationStatus.PUBLISHED),
            cb.and(
                cb.equal(root.get("status"), PublicationStatus.SCHEDULED),
                cb.lessThanOrEqualTo(root.get("publishedAt"), now)));
    }

    /**
     * Publications visibles restreintes par les critères présents du filtre (ET).
     */
    static Specification<PublicationEntity> visibleAt(Instant now, PublicationFilter filter) {
        Specification<PublicationEntity> specification = visibleAt(now);
        if (filter.hasType()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("type"), filter.type()));
        }
        if (filter.hasCategory()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("categoryId"), filter.categoryId()));
        }
        if (filter.hasTag()) {
            // Sous-requête sur publication_tag : pas de jointure, donc ni doublon ni comptage faussé.
            specification = specification.and((root, query, cb) ->
                cb.isMember(filter.tagId(), root.<Set<Long>>get("tagIds")));
        }
        return specification;
    }

    static Specification<PublicationEntity> hasSlug(String slug) {
        return (root, query, cb) -> cb.equal(root.get("slug"), slug);
    }
}
