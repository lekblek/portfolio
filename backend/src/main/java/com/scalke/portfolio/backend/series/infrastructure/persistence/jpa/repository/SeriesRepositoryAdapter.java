package com.scalke.portfolio.backend.series.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import com.scalke.portfolio.backend.series.infrastructure.persistence.jpa.mapper.SeriesPersistenceMapper;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public Series create(Series series) {
        if (series.id() != null) {
            throw new IllegalArgumentException("create expects a new series, got id " + series.id());
        }
        return SeriesPersistenceMapper.toDomain(repository.save(SeriesPersistenceMapper.toNewEntity(series)));
    }
}
