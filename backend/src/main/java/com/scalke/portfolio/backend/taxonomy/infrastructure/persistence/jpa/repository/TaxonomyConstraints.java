package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.infrastructure.persistence.ViolatedConstraint;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Traduit un refus d'unicité de PostgreSQL en erreur métier (D-CS), d'après le nom de la contrainte en cause : la
 * vérification préalable du cas d'usage a pu être devancée par une écriture concurrente.
 */
final class TaxonomyConstraints {

    private TaxonomyConstraints() {
    }

    static RuntimeException translate(DataIntegrityViolationException e) {
        return switch (ViolatedConstraint.of(e).orElse("")) {
            case "category_slug_unique", "tag_slug_unique" ->
                new BusinessRuleViolationException(ErrorCode.SLUG_ALREADY_USED, "Ce slug est déjà utilisé.");
            case "category_name_unique_idx", "tag_name_unique_idx" ->
                new BusinessRuleViolationException(ErrorCode.NAME_ALREADY_USED, "Ce nom est déjà utilisé.");
            default -> e;
        };
    }
}
