package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.model.SeriesItem;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.scalke.portfolio.backend.publication.PublicationFixtures.article;
import static com.scalke.portfolio.backend.series.SeriesFixtures.series;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@Transactional
class GetVisibleSeriesUseCaseIT extends AbstractIntegrationTest {

    @Autowired
    GetVisibleSeriesUseCase getVisibleSeriesUseCase;

    @Autowired
    SeriesRepository seriesRepository;

    @Autowired
    PublicationRepository publicationRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void givenAVisibleAndAnInvisibleSeries() {
        Long published = create("introduction", PublicationStatus.PUBLISHED, NOW.minus(Duration.ofDays(10)));
        Long draft = create("brouillon", PublicationStatus.DRAFT, null);
        Long due = create("planifie-passe", PublicationStatus.SCHEDULED, NOW.minusSeconds(1));
        Long planned = create("planifie-futur", PublicationStatus.SCHEDULED, NOW.plus(Duration.ofDays(1)));
        seriesRepository.create(new Series(null, "Spring Boot", Slug.of("spring-boot"), "## Une série",
            List.of(new SeriesItem(planned, 7), new SeriesItem(due, 5), new SeriesItem(draft, 2),
                new SeriesItem(published, 1))));
        seriesRepository.create(series("angular", create("en-relecture", PublicationStatus.IN_REVIEW, null)));
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * D-BG : seuls les articles visibles, dans l'ordre des positions, numérotés sans trou.
     */
    @Test
    void returns_the_visible_chapters_in_order_with_their_public_position() {
        VisibleSeries visible = getVisibleSeriesUseCase.execute("spring-boot");

        assertThat(visible.series().title()).isEqualTo("Spring Boot");
        assertThat(visible.chapters())
            .extracting(SeriesChapter::position, chapter -> chapter.publication().slug().value())
            .containsExactly(tuple(1, "introduction"), tuple(2, "planifie-passe"));
    }

    /**
     * Série inconnue, au slug mal formé, ou sans article visible : réponse identique (D-BG, 05 §9).
     */
    @ParameterizedTest
    @ValueSource(strings = {"angular", "inconnue", "Spring-Boot", "../spring-boot"})
    void fails_for_a_series_that_is_not_visible(String slug) {
        assertThatThrownBy(() -> getVisibleSeriesUseCase.execute(slug))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Série introuvable.");
    }

    @Test
    void a_malformed_slug_is_not_found_without_querying_the_database() {
        Statistics statistics = statistics();

        assertThatThrownBy(() -> getVisibleSeriesUseCase.execute("Spring Boot"))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    /**
     * D-BJ : série, ses articles, les articles visibles (façade) et leurs tags → 4 requêtes.
     */
    @Test
    void loads_a_series_in_a_constant_number_of_queries() {
        Statistics statistics = statistics();

        getVisibleSeriesUseCase.execute("spring-boot");

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
    }

    private Long create(String slug, PublicationStatus status, Instant publishedAt) {
        return publicationRepository.create(article(slug, status, publishedAt)).id();
    }

    private Statistics statistics() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }
}
