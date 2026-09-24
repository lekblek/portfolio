package com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationFilter;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.entity.PublicationEntity;
import com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.mapper.PublicationPersistenceMapper;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Adaptateur JPA du port {@link PublicationRepository}.
 * <p>
 * La visibilité est appliquée dans les requêtes ({@link PublicationJpaRepository#VISIBLE_AT_NOW}) pour
 * que la pagination ne compte que les publications visibles. Chaque méthode est transactionnelle pour
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

    private final PublicationJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<Publication> findVisible(PublicationFilter filter, Instant now, PageQuery query) {
        Pageable pageable = PageRequest.of(query.page(), query.size(), PUBLIC_ORDER);
        Page<PublicationEntity> entities = filter.hasType()
            ? repository.findVisibleByType(filter.type(), now, pageable)
            : repository.findVisible(now, pageable);
        Page<Publication> page = entities.map(PublicationPersistenceMapper::toDomain);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Publication> findVisibleBySlug(String slug, Instant now) {
        return repository.findVisibleBySlug(slug, now).map(PublicationPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAny() {
        return repository.count() > 0;
    }

    @Override
    @Transactional
    public Publication create(Publication publication) {
        if (publication.id() != null) {
            throw new IllegalArgumentException("create expects a new publication, got id " + publication.id());
        }
        return PublicationPersistenceMapper.toDomain(
            repository.save(PublicationPersistenceMapper.toNewEntity(publication)));
    }
}
