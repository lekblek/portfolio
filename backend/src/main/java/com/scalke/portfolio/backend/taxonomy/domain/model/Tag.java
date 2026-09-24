package com.scalke.portfolio.backend.taxonomy.domain.model;

import java.util.Comparator;

/**
 * Tag éditorial (racine d'agrégat), distinct des technologies des projets (D21).
 * <p>
 * Invariant 25 : nom (sans tenir compte de la casse) et slug uniques ; un tag utilisé par une
 * publication ne peut pas être supprimé. Garanti par PostgreSQL ({@code V007}, {@code V008}, D-AO).
 */
public record Tag(Long id, String name, String slug) {

    /**
     * Ordre d'affichage des tags (ils n'ont pas d'ordre propre) : alphabétique, puis slug (unique).
     */
    public static final Comparator<Tag> BY_NAME = Comparator
        .comparing(Tag::name, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(Tag::slug);
}
