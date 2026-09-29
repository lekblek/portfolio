package com.scalke.portfolio.backend.taxonomy.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.Slug;

/**
 * Catégorie éditoriale (racine d'agrégat). Une publication en possède au plus une (D20).
 * <p>
 * Invariant 25 : nom (sans tenir compte de la casse) et slug uniques ; une catégorie utilisée par une
 * publication ne peut pas être supprimée. Garanti par PostgreSQL ({@code V007}, {@code V008}, D-AO).
 */
public record Category(Long id, String name, Slug slug, String description) {

    public static final int NAME_MAX_LENGTH = 80;
    public static final int SLUG_MAX_LENGTH = 80;
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    /**
     * Mêmes bornes que les colonnes de {@code V007} (D-CS) ; une valeur hors bornes est une erreur de programmation
     * (la saisie est validée avant, par le contrat HTTP).
     */
    public Category {
        Terms.requireName(name, NAME_MAX_LENGTH);
        Terms.requireSlug(slug, SLUG_MAX_LENGTH);
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("description must not exceed " + DESCRIPTION_MAX_LENGTH + " characters");
        }
    }
}
