package com.scalke.portfolio.backend.shared.domain.model;

/**
 * Demande d'une page de résultats (numérotée à partir de 0), sans dépendance à Spring Data :
 * un port du domaine ne peut pas manipuler {@code Pageable} (ADR 0001, D-V).
 * <p>
 * Les bornes HTTP (taille par défaut, taille maximale) sont appliquées avant, par la couche web
 * ({@code docs/05-conventions-api.md} §13) ; une valeur hors bornes ici est une erreur de programmation.
 */
public record PageQuery(int page, int size) {

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
    }
}
