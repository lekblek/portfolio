package com.scalke.portfolio.backend.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

class PageResponseTest {
    @Test
    void maps_an_intermediate_page() {
        PageResult<String> page = new PageResult<>(List.of("b1", "b2"), 1, 2, 5);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).containsExactly("b1", "b2");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
    }

    @Test
    void maps_an_empty_page() {
        PageResult<String> page = new PageResult<>(List.of(), 0, 10, 0);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }
}
