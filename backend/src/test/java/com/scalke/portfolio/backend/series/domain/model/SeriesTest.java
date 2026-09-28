package com.scalke.portfolio.backend.series.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.Slug;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeriesTest {

    @Test
    void keeps_its_articles_in_position_order() {
        Series series = series(new SeriesItem(30L, 5), new SeriesItem(10L, 1), new SeriesItem(20L, 2));

        assertThat(series.items()).extracting(SeriesItem::position).containsExactly(1, 2, 5);
        assertThat(series.publicationIds()).containsExactly(10L, 20L, 30L);
    }

    /**
     * Invariant 3.
     */
    @Test
    void rejects_two_articles_at_the_same_position() {
        assertThatThrownBy(() -> series(new SeriesItem(10L, 1), new SeriesItem(20L, 1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("position 1");
    }

    @Test
    void rejects_the_same_article_twice() {
        assertThatThrownBy(() -> series(new SeriesItem(10L, 1), new SeriesItem(10L, 2)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("publication 10");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejects_a_position_that_is_not_positive(int position) {
        assertThatThrownBy(() -> new SeriesItem(10L, position)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * D-BG : les articles retenus gardent l'ordre des positions ; leur rang est la position publique.
     */
    @Test
    void keeps_the_order_of_the_articles_among_a_given_set() {
        Series series = series(new SeriesItem(10L, 1), new SeriesItem(20L, 2), new SeriesItem(30L, 3),
            new SeriesItem(40L, 7));

        assertThat(series.publicationIdsAmong(Set.of(40L, 10L, 30L, 99L))).containsExactly(10L, 30L, 40L);
        assertThat(series.publicationIdsAmong(Set.of())).isEmpty();
    }

    @Test
    void accepts_a_series_without_articles() {
        assertThat(series().publicationIds()).isEmpty();
    }

    @Test
    void requires_a_slug() {
        assertThatThrownBy(() -> new Series(null, "Titre", null, "Description", List.of()))
            .isInstanceOf(NullPointerException.class);
    }

    private static Series series(SeriesItem... items) {
        return new Series(null, "Spring Boot", Slug.of("spring-boot"), "Description", List.of(items));
    }
}
