package com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.InvalidInputException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Traduit un refus de PostgreSQL en erreur de la requête (D-CU), d'après le nom de la contrainte : les vérifications
 * du cas d'usage ont pu être devancées par une écriture concurrente (slug pris, terme ou média supprimé).
 */
final class PublicationConstraints {

    private PublicationConstraints() {
    }

    static RuntimeException translate(DataIntegrityViolationException e) {
        String message = String.valueOf(e.getMostSpecificCause().getMessage());
        if (message.contains("publication_slug_unique")) {
            return new BusinessRuleViolationException(ErrorCode.SLUG_ALREADY_USED, "Ce slug est déjà utilisé.");
        }
        if (message.contains("publication_category_fk")) {
            return new InvalidInputException("categoryId", "Catégorie inconnue.");
        }
        if (message.contains("publication_tag_tag_fk")) {
            return new InvalidInputException("tagIds", "Tag inconnu.");
        }
        if (message.contains("publication_cover_media_fk")) {
            return new InvalidInputException("coverMediaId", "Image de couverture inconnue.");
        }
        return e;
    }
}
