package com.scalke.portfolio.backend.series.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Ligne de {@code series_item} : composant de la série, sans identité propre (02 §20). La colonne
 * {@code publication_type} n'est pas mappée : sa valeur par défaut ({@code ARTICLE}) sert uniquement à la
 * clé étrangère composite qui interdit les {@code NEWS} (D-BF).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Embeddable
public class SeriesItemEmbeddable {

    /**
     * Identifiant d'un article du module {@code publication} : jamais une entité de ce module (ADR 0002).
     */
    @Column(name = "publication_id", nullable = false)
    private Long publicationId;

    @Column(name = "position", nullable = false)
    private int position;
}
