package com.scalke.portfolio.backend.media.application.query;

import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.scalke.portfolio.backend.media.MediaFixtures.image;
import static com.scalke.portfolio.backend.media.MediaFixtures.pdf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Façade du catalogue pour les autres modules (D-BW).
 */
@Transactional
class MediaQueryServiceIT extends AbstractIntegrationTest {

    @Autowired
    MediaQueryService mediaQueryService;

    @Autowired
    MediaRepository mediaRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void gives_the_public_form_of_images_only_in_one_query() {
        Media cover = mediaRepository.create(image("Couverture"));
        Media decorative = mediaRepository.create(image(null));
        Media document = mediaRepository.create(pdf());
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = statistics();

        Map<Long, PublicImage> images = mediaQueryService.imagesById(
            List.of(cover.id(), decorative.id(), document.id(), 999_999L));

        assertThat(images).containsOnlyKeys(cover.id(), decorative.id());
        assertThat(images.get(cover.id())).isEqualTo(new PublicImage(
            "/api/public/media/" + cover.storageKey().value(), 1200, 630, "Couverture"));
        assertThat(images.get(decorative.id()).altText()).isNull();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void gives_the_public_form_of_documents_only() {
        Media image = mediaRepository.create(image("Portrait"));
        Media document = mediaRepository.create(pdf());

        Map<Long, PublicDocument> documents = mediaQueryService.documentsById(List.of(image.id(), document.id()));

        assertThat(documents).containsOnlyKeys(document.id());
        assertThat(documents.get(document.id()))
            .isEqualTo(new PublicDocument("/api/public/media/" + document.storageKey().value(), 2_048));
        assertThat(mediaQueryService.documentsById(Set.of())).isEmpty();
    }

    @Test
    void asking_for_no_identifier_costs_no_query() {
        Statistics statistics = statistics();

        assertThat(mediaQueryService.imagesById(Set.of())).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    private Statistics statistics() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }
}
