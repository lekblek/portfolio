package com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.entity.PublicationEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PublicationPersistenceMapperTest {

    @Test
    void keeps_every_field_through_a_round_trip() {
        Publication publication = new Publication(
            null,
            PublicationType.NEWS,
            "Lancement du portfolio",
            "lancement-du-portfolio",
            "Résumé",
            "## Contenu",
            PublicationStatus.SCHEDULED,
            Instant.parse("2026-07-01T08:00:00Z"),
            true,
            7L,
            Set.of(1L, 2L),
            "Titre SEO",
            "Description SEO",
            Instant.parse("2026-06-01T09:00:00Z"),
            Instant.parse("2026-06-02T09:00:00Z"));

        PublicationEntity entity = PublicationPersistenceMapper.toNewEntity(publication);

        assertThat(entity.getId()).isNull();
        assertThat(PublicationPersistenceMapper.toDomain(entity)).isEqualTo(publication);
    }
}
