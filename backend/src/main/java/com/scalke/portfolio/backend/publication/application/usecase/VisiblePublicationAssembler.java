package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.taxonomy.application.query.TaxonomyQueryService;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Associe aux publications leurs termes de classement, en passant par la façade du module
 * {@code taxonomy} (D-AN). Deux requêtes au plus pour tout un lot de publications (catégories, tags),
 * aucune si le lot n'est pas classé (D-AS).
 * <p>
 * Partagé par les deux cas d'usage publics ; s'exécute dans leur transaction.
 */
@Component
@RequiredArgsConstructor
class VisiblePublicationAssembler {

    private final TaxonomyQueryService taxonomy;

    List<VisiblePublication> assemble(Collection<Publication> publications) {
        Set<Long> categoryIds = publications.stream()
            .map(Publication::categoryId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Set<Long> tagIds = publications.stream()
            .flatMap(publication -> publication.tagIds().stream())
            .collect(Collectors.toSet());
        Map<Long, Category> categories = taxonomy.categoriesById(categoryIds);
        Map<Long, Tag> tags = taxonomy.tagsById(tagIds);

        return publications.stream()
            .map(publication -> new VisiblePublication(
                publication,
                publication.categoryId() == null ? null : categories.get(publication.categoryId()),
                publication.tagIds().stream()
                    .map(tags::get)
                    .filter(Objects::nonNull)
                    .sorted(Tag.BY_NAME)
                    .toList()))
            .toList();
    }

    VisiblePublication assemble(Publication publication) {
        return assemble(List.of(publication)).getFirst();
    }
}
