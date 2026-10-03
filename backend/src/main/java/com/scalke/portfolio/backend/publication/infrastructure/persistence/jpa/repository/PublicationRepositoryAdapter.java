package com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationFilter;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.entity.PublicationEntity;
import com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.mapper.PublicationPersistenceMapper;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.scalke.portfolio.backend.publication.domain.model.PublicationAdminFilter;

import static com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.repository.PublicationSpecifications.administered;
import static com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.repository.PublicationSpecifications.hasIdIn;
import static com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.repository.PublicationSpecifications.hasSlug;
import static com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.repository.PublicationSpecifications.visibleAt;

/**
 * Adaptateur JPA du port {@link PublicationRepository}.
 * <p>
 * La visibilité et les filtres sont appliqués dans les requêtes ({@link PublicationSpecifications}) pour
 * que la pagination ne compte que ce qui est visible. Le mapping en records a lieu dans la transaction :
 * les tags d'une page y sont chargés en une requête (D-AS). Chaque méthode est transactionnelle pour
 * rester correcte hors cas d'usage (seed de développement) ; appelée depuis un cas d'usage, elle rejoint
 * sa transaction.
 */
@Repository
@RequiredArgsConstructor
public class PublicationRepositoryAdapter implements PublicationRepository {

    /**
     * Les plus récentes d'abord, tri total grâce à l'identifiant (D-AK).
     */
    private static final Sort PUBLIC_ORDER = Sort.by(
        Sort.Order.desc("publishedAt"),
        Sort.Order.desc("id"));

    /**
     * Administration : les dernières modifiées d'abord, tri total (D-CU).
     */
    private static final Sort ADMIN_ORDER = Sort.by(
        Sort.Order.desc("updatedAt"),
        Sort.Order.desc("id"));

    private final PublicationJpaRepository repository;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public PageResult<Publication> findVisible(PublicationFilter filter, Instant now, PageQuery query) {
        Page<Publication> page = repository
            .findAll(visibleAt(now, filter), PageRequest.of(query.page(), query.size(), PUBLIC_ORDER))
            .map(PublicationPersistenceMapper::toDomain);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Publication> findVisibleBySlug(Slug slug, Instant now) {
        return repository.findOne(visibleAt(now).and(hasSlug(slug.value()))).map(PublicationPersistenceMapper::toDomain);
    }

    /**
     * Projection sur l'identifiant, avec la même règle de visibilité que les autres lectures ({@code visibleAt}).
     */
    @Override
    @Transactional(readOnly = true)
    public Set<Long> findVisibleIds(Collection<Long> ids, Instant now) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<PublicationEntity> root = query.from(PublicationEntity.class);
        query.select(root.get("id")).where(visibleAt(now).and(hasIdIn(ids)).toPredicate(root, query, cb));
        return Set.copyOf(entityManager.createQuery(query).getResultList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Publication> findVisibleByIds(Collection<Long> ids, Instant now) {
        return repository.findAll(visibleAt(now).and(hasIdIn(ids))).stream()
            .map(PublicationPersistenceMapper::toDomain)
            .toList();
    }

    /**
     * Deux requêtes : la recherche plein texte, puis la règle de visibilité ({@code visibleAt}) sur les seuls
     * identifiants trouvés, pour que cette règle reste écrite une fois (D-AH, D-CC). Aucune seconde requête si
     * rien ne correspond.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Double> searchVisible(String text, Instant now) {
        Map<Long, Double> ranks = new HashMap<>();
        repository.search(text).forEach(row -> ranks.put(row.getId(), row.getRank().doubleValue()));
        if (!ranks.isEmpty()) {
            ranks.keySet().retainAll(findVisibleIds(Set.copyOf(ranks.keySet()), now));
        }
        return Map.copyOf(ranks);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Publication> findBySlug(Slug slug) {
        return repository.findOne(hasSlug(slug.value())).map(PublicationPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAny() {
        return repository.count() > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Publication> findPage(PublicationAdminFilter filter, Instant now, PageQuery query) {
        Page<Publication> page = repository
            .findAll(administered(filter, now), PageRequest.of(query.page(), query.size(), ADMIN_ORDER))
            .map(PublicationPersistenceMapper::toDomain);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySlug(Slug slug, Long excludedId) {
        return repository.existsBySlug(slug.value(), excludedId);
    }

    @Override
    @Transactional
    public Publication create(Publication publication) {
        if (publication.id() != null) {
            throw new IllegalArgumentException("create expects a new publication, got id " + publication.id());
        }
        try {
            return PublicationPersistenceMapper.toDomain(
                repository.saveAndFlush(PublicationPersistenceMapper.toNewEntity(publication)));
        } catch (DataIntegrityViolationException e) {
            throw PublicationConstraints.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Publication> findById(Long id) {
        return repository.findById(id).map(PublicationPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Publication> findAllById(Collection<Long> ids) {
        return repository.findAll(hasIdIn(ids)).stream().map(PublicationPersistenceMapper::toDomain).toList();
    }

    /**
     * Modifie l'entité gérée ; l'écriture a lieu à la validation de la transaction (dirty checking).
     */
    @Override
    @Transactional
    public Publication updateStatus(Publication publication) {
        PublicationEntity entity = repository.findById(publication.id())
            .orElseThrow(() -> new IllegalStateException("publication " + publication.id() + " does not exist"));
        entity.changeStatus(publication.status(), publication.publishedAt(), publication.firstPublishedAt(),
            publication.updatedAt());
        return PublicationPersistenceMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public Publication update(Publication publication) {
        PublicationEntity entity = repository.findById(publication.id())
            .orElseThrow(() -> new IllegalStateException("publication " + publication.id() + " does not exist"));
        entity.revise(publication.title(), publication.slug().value(), publication.summary(),
            publication.contentMarkdown(), publication.featured(), publication.categoryId(), publication.tagIds(),
            publication.seoTitle(), publication.seoDescription(), publication.coverMediaId(), publication.updatedAt());
        try {
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw PublicationConstraints.translate(e);
        }
        return PublicationPersistenceMapper.toDomain(entity);
    }
}
