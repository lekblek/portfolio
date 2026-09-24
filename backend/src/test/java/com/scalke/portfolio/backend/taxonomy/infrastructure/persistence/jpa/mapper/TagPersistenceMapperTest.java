package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TagPersistenceMapperTest {

    @Test
    void keeps_every_field_through_a_round_trip() {
        Tag tag = new Tag(null, "Spring Boot", "spring-boot");

        assertThat(TagPersistenceMapper.toDomain(TagPersistenceMapper.toNewEntity(tag))).isEqualTo(tag);
    }
}
