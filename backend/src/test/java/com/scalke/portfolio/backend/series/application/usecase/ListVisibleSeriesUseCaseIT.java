package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static com.scalke.portfolio.backend.publication.PublicationFixtures.article;
import static com.scalke.portfolio.backend.series.SeriesFixtures.series;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ListVisibleSeriesUseCaseIT extends AbstractIntegrationTest {

    @Autowired
    ListVisibleSeriesUseCase listVisibleSeriesUseCase;

    @Autowired
    SeriesRepository seriesRepository;

    @Autowired
    PublicationRepository publicationRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void returns_an_empty_page_when_no_series_has_a_visible_article() {
        seriesRepository.create(series("brouillons", draft("brouillon"), planned("a-venir")));
        seriesRepository.create(series("vide"));

        PageResult<SeriesSummary> page = listVisibleSeriesUseCase.execute(new PageQuery(0, 10));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    /**
     * D-BG et D-BI : seules les séries ayant un article visible, par titre sans tenir compte de la casse.
     */
    @Test
    void lists_only_series_with_a_visible_article_by_title() {
        seriesRepository.create(series("Spring Boot", "spring-boot", published("api")));
        seriesRepository.create(series("angular moderne", "angular-moderne", draft("brouillon"), published("signals")));
        seriesRepository.create(series("Brouillons", "brouillons", draft("autre-brouillon")));
        seriesRepository.create(series("Docker", "docker"));

        PageResult<SeriesSummary> page = listVisibleSeriesUseCase.execute(new PageQuery(0, 10));

        assertThat(page.content())
            .extracting(summary -> summary.series().slug().value())
            .containsExactly("angular-moderne", "spring-boot");
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void counts_only_the_visible_chapters() {
        seriesRepository.create(series("spring-boot", published("api"), draft("brouillon"), due("planifie-passe"),
            planned("planifie-futur")));

        assertThat(listVisibleSeriesUseCase.execute(new PageQuery(0, 10)).content())
            .singleElement()
            .extracting(SeriesSummary::chapterCount)
            .isEqualTo(2);
    }

    @Test
    void paginates_visible_series_only() {
        seriesRepository.create(series("A", "a", published("a1")));
        seriesRepository.create(series("B", "b", published("b1")));
        seriesRepository.create(series("C", "c", published("c1")));
        seriesRepository.create(series("Brouillons", "brouillons", draft("brouillon")));

        PageResult<SeriesSummary> page = listVisibleSeriesUseCase.execute(new PageQuery(1, 2));

        assertThat(page.content()).extracting(summary -> summary.series().slug().value()).containsExactly("c");
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.isLast()).isTrue();
    }

    /**
     * D-BJ : une page coûte 5 requêtes quel que soit le nombre de séries : articles des séries, articles
     * visibles (façade), séries, comptage, articles de toute la page ({@code @BatchSize}).
     */
    @Test
    void loads_a_page_in_a_constant_number_of_queries() {
        for (int i = 0; i < 4; i++) {
            seriesRepository.create(series("serie-" + i, published("article-" + i), draft("brouillon-" + i)));
        }
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        PageResult<SeriesSummary> page = listVisibleSeriesUseCase.execute(new PageQuery(0, 3));

        assertThat(page.content()).hasSize(3).allSatisfy(summary -> assertThat(summary.chapterCount()).isEqualTo(1));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(5);
    }

    private Long published(String slug) {
        return publicationRepository.create(article(slug, PublicationStatus.PUBLISHED, NOW.minus(Duration.ofDays(1)))).id();
    }

    private Long due(String slug) {
        return publicationRepository.create(article(slug, PublicationStatus.SCHEDULED, NOW.minusSeconds(1))).id();
    }

    private Long planned(String slug) {
        return publicationRepository.create(article(slug, PublicationStatus.SCHEDULED, NOW.plusSeconds(1))).id();
    }

    private Long draft(String slug) {
        return publicationRepository.create(article(slug, PublicationStatus.DRAFT, null)).id();
    }
}
