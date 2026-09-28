package com.scalke.portfolio.backend.series.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.model.SeriesItem;
import com.scalke.portfolio.backend.series.infrastructure.persistence.jpa.entity.SeriesEntity;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SeriesPersistenceMapperTest {

    @Test
    void keeps_every_field_through_a_round_trip() {
        Series series = new Series(null, "Spring Boot de zéro à la production",
            Slug.of("spring-boot-de-zero-a-la-production"), "## Description",
            List.of(new SeriesItem(7L, 1), new SeriesItem(3L, 4)));

        SeriesEntity entity = SeriesPersistenceMapper.toNewEntity(series);

        assertThat(entity.getId()).isNull();
        assertThat(SeriesPersistenceMapper.toDomain(entity)).isEqualTo(series);
    }
}
