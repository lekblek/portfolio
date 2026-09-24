package com.scalke.portfolio.backend.shared.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageQueryTest {

    @Test
    void rejects_a_negative_page() {
        assertThatThrownBy(() -> new PageQuery(-1, 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_an_empty_page_size() {
        assertThatThrownBy(() -> new PageQuery(0, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
