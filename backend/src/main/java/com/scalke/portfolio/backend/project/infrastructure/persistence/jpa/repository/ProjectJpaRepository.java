package com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Accès Spring Data aux projets. Seules les méthodes utilisées par l'adaptateur sont déclarées.
 */
public interface ProjectJpaRepository extends Repository<ProjectEntity, Long> {

    Page<ProjectEntity> findByVisibility(ProjectVisibility visibility, Pageable pageable);

    /**
     * Jointure sur {@code technologies} : le slug d'une technologie étant unique, un projet apparaît au
     * plus une fois, ce qui garde la pagination et le comptage exacts (D-AC).
     */
    Page<ProjectEntity> findByVisibilityAndTechnologiesSlug(
        ProjectVisibility visibility, String technologySlug, Pageable pageable);

    /**
     * Toutes visibilités (administration), même jointure que ci-dessus (D-AC).
     */
    Page<ProjectEntity> findByTechnologiesSlug(String technologySlug, Pageable pageable);

    Page<ProjectEntity> findByVisibilityAndFeatured(ProjectVisibility visibility, boolean featured, Pageable pageable);

    Page<ProjectEntity> findByVisibilityAndFeaturedAndTechnologiesSlug(
        ProjectVisibility visibility, boolean featured, String technologySlug, Pageable pageable);

    Optional<ProjectEntity> findBySlugAndVisibility(String slug, ProjectVisibility visibility);

    List<ProjectEntity> findByVisibilityAndIdIn(ProjectVisibility visibility, Collection<Long> ids);

    /**
     * Pertinence de chaque projet de cette visibilité dont le document ({@code V019}) correspond à {@code text}
     * lu comme une recherche web (D-CC). La visibilité est un paramètre, comme dans les requêtes dérivées
     * (D-U) ; le filtre {@code @@} passe par l'index GIN.
     */
    @Query(value = """
        SELECT p.id AS id, ts_rank(p.search_vector, query) AS rank
          FROM project p, websearch_to_tsquery('french_unaccent', :text) AS query
         WHERE p.visibility = :visibility
           AND p.search_vector @@ query
        """, nativeQuery = true)
    List<SearchRank> search(String text, String visibility);

    long count();

    Page<ProjectEntity> findAll(Pageable pageable);

    Optional<ProjectEntity> findById(Long id);

    @Query("select count(p) > 0 from ProjectEntity p where p.slug = :slug and (:excludedId is null or p.id <> :excludedId)")
    boolean existsBySlug(String slug, Long excludedId);

    ProjectEntity saveAndFlush(ProjectEntity entity);

    void flush();

    /**
     * Ligne de {@link #search} : {@code ts_rank} renvoie un {@code real}.
     */
    interface SearchRank {

        Long getId();

        Float getRank();
    }
}
