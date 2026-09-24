package com.scalke.portfolio.backend.taxonomy.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TagTest {

    @Test
    void tags_are_displayed_alphabetically_ignoring_case() {
        Tag spring = new Tag(1L, "Spring Boot", "spring-boot");
        Tag angular = new Tag(2L, "angular", "angular");
        Tag java = new Tag(3L, "Java", "java");

        assertThat(List.of(spring, java, angular).stream().sorted(Tag.BY_NAME).toList())
            .containsExactly(angular, java, spring);
    }
}
