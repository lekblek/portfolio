package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
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

import static com.scalke.portfolio.backend.publication.PublicationFixtures.article;
import static com.scalke.portfolio.backend.publication.PublicationFixtures.publication;
import static com.scalke.portfolio.backend.series.SeriesFixtures.series;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class GetSeriesNavigationUseCaseIT extends AbstractIntegrationTest {

    @Autowired
    GetSeriesNavigationUseCase getSeriesNavigationUseCase;

    @Autowired
    SeriesRepository seriesRepository;

    @Autowired
    PublicationRepository publicationRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    /**
     * Série de cinq articles dont deux invisibles (brouillon en 2, planifié futur en 5) : trois chapitres
     * publics, « introduction » (1), « configuration » (2), « deploiement » (3).
     */
    @BeforeEach
    void givenASeriesWithHiddenArticles() {
        seriesRepository.create(series("spring-boot",
            create("introduction", PublicationStatus.PUBLISHED, NOW.minus(Duration.ofDays(10))),
            create("brouillon", PublicationStatus.DRAFT, null),
            create("configuration", PublicationStatus.SCHEDULED, NOW.minusSeconds(1)),
            create("deploiement", PublicationStatus.PUBLISHED, NOW.minus(Duration.ofDays(1))),
            create("a-venir", PublicationStatus.SCHEDULED, NOW.plus(Duration.ofDays(1)))));
        create("article-isole", PublicationStatus.PUBLISHED, NOW.minus(Duration.ofDays(2)));
        publicationRepository.create(publication("annonce", PublicationType.NEWS, PublicationStatus.PUBLISHED,
            NOW.minus(Duration.ofDays(2))));
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * D-BM : les articles invisibles sont sautés, la position est le rang parmi les visibles.
     */
    @Test
    void links_a_chapter_to_its_visible_neighbours() {
        SeriesNavigation navigation = getSeriesNavigationUseCase.execute("configuration");

        assertThat(navigation.series().slug().value()).isEqualTo("spring-boot");
        assertThat(navigation.position()).isEqualTo(2);
        assertThat(navigation.chapterCount()).isEqualTo(3);
        assertThat(navigation.previous().position()).isEqualTo(1);
        assertThat(navigation.previous().publication().slug().value()).isEqualTo("introduction");
        assertThat(navigation.next().position()).isEqualTo(3);
        assertThat(navigation.next().publication().slug().value()).isEqualTo("deploiement");
    }

    @Test
    void the_first_and_last_visible_chapters_have_a_single_neighbour() {
        SeriesNavigation first = getSeriesNavigationUseCase.execute("introduction");
        SeriesNavigation last = getSeriesNavigationUseCase.execute("deploiement");

        assertThat(first.previous()).isNull();
        assertThat(first.next().publication().slug().value()).isEqualTo("configuration");
        assertThat(last.position()).isEqualTo(3);
        assertThat(last.previous().publication().slug().value()).isEqualTo("configuration");
        assertThat(last.next()).isNull();
    }

    /**
     * Une publication invisible, inconnue ou au slug mal formé : même réponse que la lecture publique.
     */
    @ParameterizedTest
    @ValueSource(strings = {"brouillon", "a-venir", "inconnue", "Configuration"})
    void fails_for_a_publication_that_is_not_visible(String slug) {
        assertThatThrownBy(() -> getSeriesNavigationUseCase.execute(slug))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Publication introuvable.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"article-isole", "annonce"})
    void fails_for_a_visible_publication_outside_any_series(String slug) {
        assertThatThrownBy(() -> getSeriesNavigationUseCase.execute(slug))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Cette publication n'appartient à aucune série.");
    }

    @Test
    void a_malformed_slug_is_not_found_without_querying_the_database() {
        Statistics statistics = statistics();

        assertThatThrownBy(() -> getSeriesNavigationUseCase.execute("../configuration"))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    /**
     * D-BN : article visible et ses tags, série et ses articles, articles visibles et leurs tags → 6 requêtes,
     * quelle que soit la longueur de la série.
     */
    @Test
    void navigates_in_a_constant_number_of_queries() {
        Statistics statistics = statistics();

        getSeriesNavigationUseCase.execute("configuration");

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(6);
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
