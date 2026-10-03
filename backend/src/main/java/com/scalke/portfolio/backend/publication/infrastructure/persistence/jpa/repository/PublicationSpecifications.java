package com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.publication.domain.model.PublicationFilter;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.entity.PublicationEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import com.scalke.portfolio.backend.publication.domain.model.PublicationAdminFilter;

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

    /**
     * Liste d'administration (F28) : tout statut, restreint par type et par statut observable à {@code now} :
     * {@code PUBLISHED} comprend les planifiées échues ({@link #visibleAt(Instant)}), {@code SCHEDULED} ne garde que
     * les planifiées à venir.
     */
    static Specification<PublicationEntity> administered(PublicationAdminFilter filter, Instant now) {
        Specification<PublicationEntity> specification = (root, query, cb) -> cb.conjunction();
        if (filter.hasType()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("type"), filter.type()));
        }
        if (filter.hasStatus()) {
            Specification<PublicationEntity> status = switch (filter.status()) {
                case PUBLISHED -> visibleAt(now);
                case SCHEDULED -> (root, query, cb) -> cb.and(
                    cb.equal(root.get("status"), PublicationStatus.SCHEDULED),
                    cb.greaterThan(root.get("publishedAt"), now));
                default -> (root, query, cb) -> cb.equal(root.get("status"), filter.status());
            };
            specification = specification.and(status);
        }
        return specification;
    }

    static Specification<PublicationEntity> hasSlug(String slug) {
        return (root, query, cb) -> cb.equal(root.get("slug"), slug);
    }

    static Specification<PublicationEntity> hasIdIn(Collection<Long> ids) {
        return (root, query, cb) -> root.get("id").in(ids);
    }
}
