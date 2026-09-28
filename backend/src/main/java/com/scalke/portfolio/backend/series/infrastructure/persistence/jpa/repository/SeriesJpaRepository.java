package com.scalke.portfolio.backend.series.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.series.infrastructure.persistence.jpa.entity.SeriesEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Accès Spring Data aux séries. Seules les méthodes utilisées par l'adaptateur sont déclarées. Les requêtes
 * ne lisent que les tables du module ({@code series}, {@code series_item}) : la visibilité des articles
 * arrive sous forme d'identifiants (ADR 0002).
 */
public interface SeriesJpaRepository extends Repository<SeriesEntity, Long> {

    @Query("select item.publicationId from SeriesEntity series join series.items item")
    List<Long> findAllPublicationIds();

    /**
     * Sous-requête plutôt que jointure : une série apparaît une fois, quel que soit le nombre de ses articles
     * retenus, ce qui garde la pagination et le comptage exacts.
     */
    @Query(value = """
        select series from SeriesEntity series
        where series.id in (
            select candidate.id from SeriesEntity candidate join candidate.items item
            where item.publicationId in :publicationIds)
        order by lower(series.title), series.id
        """,
        countQuery = """
        select count(series) from SeriesEntity series
        where series.id in (
            select candidate.id from SeriesEntity candidate join candidate.items item
            where item.publicationId in :publicationIds)
        """)
    Page<SeriesEntity> findHavingAnyPublication(Collection<Long> publicationIds, Pageable pageable);

    Optional<SeriesEntity> findBySlug(String slug);

    /**
     * Au plus une ligne : un article n'appartient qu'à une série ({@code series_item_publication_unique}).
     */
    @Query("select series from SeriesEntity series join series.items item where item.publicationId = :publicationId")
    Optional<SeriesEntity> findByPublicationId(Long publicationId);

    long count();

    SeriesEntity save(SeriesEntity entity);
}
