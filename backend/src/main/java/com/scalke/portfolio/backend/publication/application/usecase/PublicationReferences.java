package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.media.application.query.MediaQueryService;
import com.scalke.portfolio.backend.publication.domain.model.PublicationContent;
import com.scalke.portfolio.backend.shared.error.InvalidInputException;
import com.scalke.portfolio.backend.taxonomy.application.query.TaxonomyQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Vérifie, par les façades des modules propriétaires (ADR 0002), que la saisie ne référence que des termes existants
 * et une image du catalogue (D-CU) : sinon 400 {@code VALIDATION_FAILED} sur le champ, plutôt qu'un refus de clé
 * étrangère. Un PDF n'est pas une couverture : aucune contrainte SQL ne le dirait (D-BV).
 */
@Component
@RequiredArgsConstructor
class PublicationReferences {

    private final TaxonomyQueryService taxonomyQueryService;
    private final MediaQueryService mediaQueryService;

    void verify(PublicationContent content) {
        Long categoryId = content.categoryId();
        if (categoryId != null && !taxonomyQueryService.categoriesById(Set.of(categoryId)).containsKey(categoryId)) {
            throw new InvalidInputException("categoryId", "Catégorie inconnue.");
        }
        if (!taxonomyQueryService.tagsById(content.tagIds()).keySet().containsAll(content.tagIds())) {
            throw new InvalidInputException("tagIds", "Tag inconnu.");
        }
        Long coverMediaId = content.coverMediaId();
        if (coverMediaId != null && !mediaQueryService.imagesById(Set.of(coverMediaId)).containsKey(coverMediaId)) {
            throw new InvalidInputException("coverMediaId", "Image de couverture inconnue.");
        }
    }
}
