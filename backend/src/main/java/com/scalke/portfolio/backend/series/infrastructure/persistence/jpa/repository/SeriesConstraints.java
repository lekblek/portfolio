package com.scalke.portfolio.backend.series.infrastructure.persistence.jpa.repository;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.InvalidInputException;
import com.scalke.portfolio.backend.shared.infrastructure.persistence.ViolatedConstraint;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Traduit un refus de PostgreSQL en erreur de la requête (D-CV), d'après le nom de la contrainte : les vérifications
 * du cas d'usage ont pu être devancées par une écriture concurrente (slug pris, article rangé dans une autre série,
 * média supprimé).
 */
final class SeriesConstraints {

    private SeriesConstraints() {
    }

    static RuntimeException translate(DataIntegrityViolationException e) {
        return switch (ViolatedConstraint.of(e).orElse("")) {
            case "series_slug_unique" ->
                new BusinessRuleViolationException(ErrorCode.SLUG_ALREADY_USED, "Ce slug est déjà utilisé.");
            case "series_item_publication_unique" ->
                new BusinessRuleViolationException(ErrorCode.ARTICLE_ALREADY_IN_SERIES,
                    "Un article appartient déjà à une autre série.");
            case "series_cover_media_fk" ->
                new InvalidInputException("coverMediaId", "Image de couverture inconnue.");
            default -> e;
        };
    }
}
