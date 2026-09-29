package com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.InvalidInputException;
import com.scalke.portfolio.backend.shared.infrastructure.persistence.ViolatedConstraint;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Traduit un refus de PostgreSQL en erreur de la requête (D-CU), d'après le nom de la contrainte : les vérifications
 * du cas d'usage ont pu être devancées par une écriture concurrente (slug pris, terme ou média supprimé).
 */
final class PublicationConstraints {

    private PublicationConstraints() {
    }

    static RuntimeException translate(DataIntegrityViolationException e) {
        return switch (ViolatedConstraint.of(e).orElse("")) {
            case "publication_slug_unique" ->
                new BusinessRuleViolationException(ErrorCode.SLUG_ALREADY_USED, "Ce slug est déjà utilisé.");
            case "publication_category_fk" ->
                new InvalidInputException("categoryId", "Catégorie inconnue.");
            case "publication_tag_tag_fk" ->
                new InvalidInputException("tagIds", "Tag inconnu.");
            case "publication_cover_media_fk" ->
                new InvalidInputException("coverMediaId", "Image de couverture inconnue.");
            default -> e;
        };
    }
}
