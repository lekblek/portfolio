package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryPersistenceMapperTest {

    @Test
    void keeps_every_field_through_a_round_trip() {
        Category category = new Category(null, "Backend", "backend", "Spring Boot, API, persistance.");

        assertThat(CategoryPersistenceMapper.toDomain(CategoryPersistenceMapper.toNewEntity(category)))
            .isEqualTo(category);
    }
}
