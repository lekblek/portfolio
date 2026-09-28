package com.scalke.portfolio.backend.series.domain.model;

import java.util.Comparator;
import java.util.Objects;

/**
 * Place d'un article dans une série (D05). {@code publicationId} référence un {@code ARTICLE} du module
 * {@code publication}, par identifiant (ADR 0002) ; {@code position} est strictement positive (doublé par
 * {@code series_item_position_check}). Les positions peuvent laisser des trous : seul leur ordre compte.
 */
public record SeriesItem(Long publicationId, int position) {

    static final Comparator<SeriesItem> BY_POSITION = Comparator.comparingInt(SeriesItem::position);

    public SeriesItem {
        Objects.requireNonNull(publicationId, "publicationId");
        if (position < 1) {
            throw new IllegalArgumentException("position must be positive, got " + position);
        }
    }
}
