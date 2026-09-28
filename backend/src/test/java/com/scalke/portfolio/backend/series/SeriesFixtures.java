package com.scalke.portfolio.backend.series;

import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.model.SeriesItem;
import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.util.ArrayList;
import java.util.List;

/**
 * Séries du domaine pour les tests. Les articles sont désignés par identifiant (publications déjà créées) et
 * rangés aux positions 1, 2, … dans l'ordre donné.
 */
public final class SeriesFixtures {

    private SeriesFixtures() {
    }

    public static Series series(String slug, Long... publicationIds) {
        return series("Série " + slug, slug, publicationIds);
    }

    public static Series series(String title, String slug, Long... publicationIds) {
        List<SeriesItem> items = new ArrayList<>();
        for (Long publicationId : publicationIds) {
            items.add(new SeriesItem(publicationId, items.size() + 1));
        }
        return new Series(null, title, Slug.of(slug), "Description de " + slug + ".", items);
    }
}
