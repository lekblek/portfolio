package com.scalke.portfolio.backend.series.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import com.scalke.portfolio.backend.series.infrastructure.persistence.jpa.entity.SeriesEntity;
import com.scalke.portfolio.backend.series.infrastructure.persistence.jpa.mapper.SeriesPersistenceMapper;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Adaptateur JPA du port {@link SeriesRepository}. Le mapping en records a lieu dans la transaction : les
 * articles d'une page de séries y sont chargés en une requête (D-BJ). Chaque méthode est transactionnelle
 * pour rester correcte hors cas d'usage (seed de développement) ; appelée depuis un cas d'usage, elle
 * rejoint sa transaction.
 */
@Repository
@RequiredArgsConstructor
public class SeriesRepositoryAdapter implements SeriesRepository {

    private final SeriesJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findAllPublicationIds() {
        return Set.copyOf(repository.findAllPublicationIds());
    }

    /**
     * L'ordre est porté par la requête ({@link SeriesJpaRepository#findHavingAnyPublication}) ; la page
     * demandée n'en ajoute pas.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<Series> findHavingAnyPublication(Set<Long> publicationIds, PageQuery query) {
        Page<Series> page = repository
            .findHavingAnyPublication(publicationIds, PageRequest.of(query.page(), query.size()))
            .map(SeriesPersistenceMapper::toDomain);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Series> findBySlug(Slug slug) {
        return repository.findBySlug(slug.value()).map(SeriesPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Series> findByPublicationId(Long publicationId) {
        return repository.findByPublicationId(publicationId).map(SeriesPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAny() {
        return repository.count() > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Series> findPage(PageQuery query) {
        Page<Series> page = repository
            .findAllByTitle(PageRequest.of(query.page(), query.size()))
            .map(SeriesPersistenceMapper::toDomain);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Series> findById(Long id) {
        return repository.findById(id).map(SeriesPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySlug(Slug slug, Long excludedId) {
        return repository.existsBySlug(slug.value(), excludedId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findPublicationIdsInOtherSeries(Collection<Long> publicationIds, Long seriesId) {
        if (publicationIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(repository.findPublicationIdsInOtherSeries(publicationIds, seriesId));
    }

    @Override
    @Transactional
    public Series create(Series series) {
        if (series.id() != null) {
            throw new IllegalArgumentException("create expects a new series, got id " + series.id());
        }
        try {
            return SeriesPersistenceMapper.toDomain(repository.saveAndFlush(SeriesPersistenceMapper.toNewEntity(series)));
        } catch (DataIntegrityViolationException e) {
            throw SeriesConstraints.translate(e);
        }
    }

    @Override
    @Transactional
    public Series update(Series series) {
        SeriesEntity entity = repository.findById(series.id())
            .orElseThrow(() -> new IllegalStateException("series " + series.id() + " does not exist"));
        entity.revise(series.title(), series.slug().value(), series.descriptionMarkdown(),
            SeriesPersistenceMapper.items(series), series.coverMediaId());
        try {
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw SeriesConstraints.translate(e);
        }
        return SeriesPersistenceMapper.toDomain(entity);
    }
}
