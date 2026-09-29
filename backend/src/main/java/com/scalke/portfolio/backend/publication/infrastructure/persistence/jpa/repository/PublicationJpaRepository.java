package com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.entity.PublicationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Accès Spring Data aux publications. Les lectures publiques passent par des critères combinables
 * ({@link PublicationSpecifications}, D-AR) ; seules les autres méthodes utilisées sont déclarées.
 */
public interface PublicationJpaRepository
    extends Repository<PublicationEntity, Long>, JpaSpecificationExecutor<PublicationEntity> {

    /**
     * Pertinence de chaque publication, quel que soit son statut, dont le document ({@code V018}) correspond à
     * {@code text} lu comme une recherche web (D-CC). Le filtre {@code @@} passe par l'index GIN ; la visibilité
     * est appliquée ensuite par l'adaptateur avec la règle du module, jamais réécrite ici (D-AH).
     */
    @Query(value = """
        SELECT p.id AS id, ts_rank(p.search_vector, query) AS rank
          FROM publication p, websearch_to_tsquery('french_unaccent', :text) AS query
         WHERE p.search_vector @@ query
        """, nativeQuery = true)
    List<SearchRank> search(String text);

    long count();

    Optional<PublicationEntity> findById(Long id);

    Page<PublicationEntity> findAll(Pageable pageable);

    @Query("select count(p) > 0 from PublicationEntity p where p.slug = :slug and (:excludedId is null or p.id <> :excludedId)")
    boolean existsBySlug(String slug, Long excludedId);

    PublicationEntity saveAndFlush(PublicationEntity entity);

    void flush();

    /**
     * Ligne de {@link #search} : {@code ts_rank} renvoie un {@code real}.
     */
    interface SearchRank {

        Long getId();

        Float getRank();
    }
}
