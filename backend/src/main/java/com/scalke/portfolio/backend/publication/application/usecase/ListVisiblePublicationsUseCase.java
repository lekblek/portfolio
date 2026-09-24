package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationFilter;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.taxonomy.application.query.TaxonomyQueryService;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ListVisiblePublicationsUseCase {

    private final PublicationRepository publicationRepository;
    private final TaxonomyQueryService taxonomy;
    private final VisiblePublicationAssembler assembler;
    private final Clock clock;

    /**
     * La visibilité est calculée à la lecture (D03) avec l'horloge applicative. Un slug de catégorie ou de
     * tag inconnu donne une page vide sans interroger les publications (D-AQ).
     */
    @Transactional(readOnly = true)
    public PageResult<VisiblePublication> execute(PublicationCriteria criteria, PageQuery query) {
        Optional<PublicationFilter> filter = resolve(criteria);
        if (filter.isEmpty()) {
            return PageResult.empty(query);
        }
        PageResult<Publication> page = publicationRepository.findVisible(filter.get(), clock.instant(), query);
        return new PageResult<>(assembler.assemble(page.content()), page.page(), page.size(), page.totalElements());
    }

    /**
     * Traduit les slugs en identifiants ; vide si un slug demandé n'existe pas.
     */
    private Optional<PublicationFilter> resolve(PublicationCriteria criteria) {
        Long categoryId = null;
        if (criteria.categorySlug() != null) {
            Optional<Category> category = taxonomy.findCategoryBySlug(criteria.categorySlug());
            if (category.isEmpty()) {
                return Optional.empty();
            }
            categoryId = category.get().id();
        }
        Long tagId = null;
        if (criteria.tagSlug() != null) {
            Optional<Tag> tag = taxonomy.findTagBySlug(criteria.tagSlug());
            if (tag.isEmpty()) {
                return Optional.empty();
            }
            tagId = tag.get().id();
        }
        return Optional.of(new PublicationFilter(criteria.type(), categoryId, tagId));
    }
}
