package com.scalke.portfolio.backend.taxonomy.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.Slug;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    /**
     * D-CS : mêmes bornes que les colonnes de {@code V007}.
     */
    @Test
    void is_bounded_by_its_columns() {
        assertThat(new Category(1L, "c".repeat(80), Slug.of("c".repeat(80)), "d".repeat(500)).description()).hasSize(500);

        assertThatThrownBy(() -> new Category(1L, "c".repeat(81), Slug.of("c"), null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Category(1L, "", Slug.of("c"), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Category(1L, "C", Slug.of("c".repeat(81)), null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Category(1L, "C", Slug.of("c"), "d".repeat(501)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
