package com.scalke.portfolio.backend.project.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectFilterTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void an_absent_or_blank_technology_means_no_filter(String technology) {
        assertThat(ProjectFilter.byTechnology(technology)).isEqualTo(ProjectFilter.none());
        assertThat(ProjectFilter.byTechnology(technology).hasTechnology()).isFalse();
    }

    @Test
    void keeps_the_technology_slug_without_surrounding_spaces() {
        ProjectFilter filter = ProjectFilter.byTechnology(" java ");

        assertThat(filter.hasTechnology()).isTrue();
        assertThat(filter.technologySlug()).isEqualTo("java");
    }
}
