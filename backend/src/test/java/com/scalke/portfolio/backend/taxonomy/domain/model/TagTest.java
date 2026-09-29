package com.scalke.portfolio.backend.taxonomy.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.Slug;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagTest {

    @Test
    void tags_are_displayed_alphabetically_ignoring_case() {
        Tag spring = new Tag(1L, "Spring Boot", Slug.of("spring-boot"));
        Tag angular = new Tag(2L, "angular", Slug.of("angular"));
        Tag java = new Tag(3L, "Java", Slug.of("java"));

        assertThat(List.of(spring, java, angular).stream().sorted(Tag.BY_NAME).toList())
            .containsExactly(angular, java, spring);
    }

    /**
     * D-CS : mêmes bornes que les colonnes de {@code V007}.
     */
    @Test
    void is_bounded_by_its_columns() {
        assertThat(new Tag(1L, "t".repeat(60), Slug.of("t".repeat(60))).name()).hasSize(60);

        assertThatThrownBy(() -> new Tag(1L, "t".repeat(61), Slug.of("t"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Tag(1L, "  ", Slug.of("t"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Tag(1L, "Tag", Slug.of("t".repeat(61)))).isInstanceOf(IllegalArgumentException.class);
    }
}
