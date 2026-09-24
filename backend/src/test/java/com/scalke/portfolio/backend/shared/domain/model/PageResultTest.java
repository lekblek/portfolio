package com.scalke.portfolio.backend.shared.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageResultTest {

    @Test
    void the_last_page_may_be_partial() {
        PageResult<String> page = new PageResult<>(List.of("e5"), 2, 2, 5);

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.isFirst()).isFalse();
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void map_converts_the_content_and_keeps_the_metadata() {
        PageResult<Integer> page = new PageResult<>(List.of("a", "bb"), 0, 2, 3).map(String::length);

        assertThat(page.content()).containsExactly(1, 2);
        assertThat(page).extracting(PageResult::page, PageResult::size, PageResult::totalElements)
            .containsExactly(0, 2, 3L);
    }

    @Test
    void rejects_a_negative_total() {
        assertThatThrownBy(() -> new PageResult<>(List.of(), 0, 10, -1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
