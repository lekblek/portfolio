package com.scalke.portfolio.backend.search.domain.model;

import com.scalke.portfolio.backend.search.domain.model.SearchHit.Source;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchHitTest {

    private static final SearchHit STRONG_PROJECT = new SearchHit(Source.PROJECT, 1, 0.6);
    private static final SearchHit OLD_PUBLICATION = new SearchHit(Source.PUBLICATION, 3, 0.2);
    private static final SearchHit RECENT_PUBLICATION = new SearchHit(Source.PUBLICATION, 8, 0.2);
    private static final SearchHit TIED_PROJECT = new SearchHit(Source.PROJECT, 9, 0.2);
    private static final SearchHit WEAK_PUBLICATION = new SearchHit(Source.PUBLICATION, 5, 0.06);

    private static final List<SearchHit> SHUFFLED =
        List.of(WEAK_PUBLICATION, TIED_PROJECT, OLD_PUBLICATION, STRONG_PROJECT, RECENT_PUBLICATION);

    /**
     * Pertinence décroissante ; à égalité, les publications avant les projets, puis la plus récente.
     */
    @Test
    void orders_by_relevance_then_publications_first_then_most_recent_first() {
        assertThat(SHUFFLED.stream().sorted(SearchHit.BY_RELEVANCE))
            .containsExactly(STRONG_PROJECT, RECENT_PUBLICATION, OLD_PUBLICATION, TIED_PROJECT, WEAK_PUBLICATION);
    }

    @Test
    void keeps_the_requested_page_and_counts_every_hit() {
        PageResult<SearchHit> page = SearchHit.page(SHUFFLED, new PageQuery(1, 2));

        assertThat(page.content()).containsExactly(OLD_PUBLICATION, TIED_PROJECT);
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void a_page_beyond_the_last_one_is_empty_but_still_counts_every_hit() {
        PageResult<SearchHit> page = SearchHit.page(SHUFFLED, new PageQuery(3, 2));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(5);
    }

    @Test
    void rejects_a_negative_or_undefined_relevance() {
        assertThatThrownBy(() -> new SearchHit(Source.PUBLICATION, 1, -0.1))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SearchHit(Source.PUBLICATION, 1, Double.NaN))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
