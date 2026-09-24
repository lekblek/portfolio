package com.scalke.portfolio.backend.profile.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {

    @Test
    void rejects_an_end_before_the_start() {
        assertThatThrownBy(() -> DateRange.between(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 1, 1)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accepts_a_single_day_range() {
        LocalDate day = LocalDate.of(2024, 6, 1);

        assertThat(DateRange.between(day, day).isOngoing()).isFalse();
    }

    @Test
    void an_open_range_is_ongoing() {
        assertThat(DateRange.ongoingSince(LocalDate.of(2024, 1, 1)).isOngoing()).isTrue();
    }
}
