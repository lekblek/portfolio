package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Traduit un refus d'unicité de PostgreSQL en erreur métier (D-CS), d'après le nom de la contrainte en cause : la
 * vérification préalable du cas d'usage a pu être devancée par une écriture concurrente.
 */
final class TaxonomyConstraints {

    private TaxonomyConstraints() {
    }

    static RuntimeException translate(DataIntegrityViolationException e) {
        String message = String.valueOf(e.getMostSpecificCause().getMessage());
        if (message.contains("_slug_unique")) {
            return new BusinessRuleViolationException(ErrorCode.SLUG_ALREADY_USED, "Ce slug est déjà utilisé.");
        }
        if (message.contains("_name_unique_idx")) {
            return new BusinessRuleViolationException(ErrorCode.NAME_ALREADY_USED, "Ce nom est déjà utilisé.");
        }
        return e;
    }
}
