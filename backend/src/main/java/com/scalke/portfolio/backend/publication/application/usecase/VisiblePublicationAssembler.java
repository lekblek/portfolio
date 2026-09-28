package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.media.application.query.MediaQueryService;
import com.scalke.portfolio.backend.media.application.query.PublicImage;
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
 * Associe aux publications leurs termes de classement et leur couverture, par les façades des modules
 * {@code taxonomy} (D-AN) et {@code media} (D-BY). Trois requêtes au plus pour tout un lot de publications
 * (catégories, tags, images), aucune pour ce que le lot n'utilise pas (D-AS).
 * <p>
 * Partagé par les deux cas d'usage publics ; s'exécute dans leur transaction.
 */
@Component
@RequiredArgsConstructor
class VisiblePublicationAssembler {

    private final TaxonomyQueryService taxonomy;
    private final MediaQueryService media;

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
        Map<Long, PublicImage> covers = media.imagesById(publications.stream()
            .map(Publication::coverMediaId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()));

        return publications.stream()
            .map(publication -> new VisiblePublication(
                publication,
                publication.categoryId() == null ? null : categories.get(publication.categoryId()),
                publication.tagIds().stream()
                    .map(tags::get)
                    .filter(Objects::nonNull)
                    .sorted(Tag.BY_NAME)
                    .toList(),
                publication.coverMediaId() == null ? null : covers.get(publication.coverMediaId())))
            .toList();
    }

    VisiblePublication assemble(Publication publication) {
        return assemble(List.of(publication)).getFirst();
    }
}
