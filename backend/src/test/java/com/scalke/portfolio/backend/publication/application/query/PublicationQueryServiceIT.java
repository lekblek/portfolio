package com.scalke.portfolio.backend.publication.application.query;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.scalke.portfolio.backend.publication.PublicationFixtures.article;
import static com.scalke.portfolio.backend.publication.PublicationFixtures.publication;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Façade de lecture pour les autres modules (D-BH) : même règle de visibilité que les lectures publiques,
 * évaluée à l'horloge fixe des tests.
 */
@Transactional
class PublicationQueryServiceIT extends AbstractIntegrationTest {

    @Autowired
    PublicationQueryService publicationQueryService;

    @Autowired
    PublicationRepository publicationRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private Publication published;
    private Publication due;
    private Publication planned;
    private Publication draft;
    private Publication news;

    @BeforeEach
    void givenPublicationsOfEveryVisibility() {
        published = publicationRepository.create(article("publiee", PublicationStatus.PUBLISHED, NOW.minus(Duration.ofDays(3))));
        due = publicationRepository.create(article("planifiee-passee", PublicationStatus.SCHEDULED, NOW.minusSeconds(1)));
        planned = publicationRepository.create(article("planifiee-future", PublicationStatus.SCHEDULED, NOW.plusSeconds(1)));
        draft = publicationRepository.create(article("brouillon", PublicationStatus.DRAFT, null));
        news = publicationRepository.create(publication("annonce", PublicationType.NEWS, PublicationStatus.PUBLISHED,
            NOW.minus(Duration.ofDays(1))));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void keeps_only_the_visible_identifiers_among_those_requested() {
        Statistics statistics = statistics();

        Set<Long> visible = publicationQueryService.visibleIds(
            List.of(published.id(), due.id(), planned.id(), draft.id(), 999_999L));

        assertThat(visible).containsExactlyInAnyOrder(published.id(), due.id());
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void loads_the_visible_publications_among_those_requested() {
        Map<Long, Publication> visible = publicationQueryService.visibleById(
            List.of(published.id(), planned.id(), draft.id(), news.id()));

        assertThat(visible).containsOnlyKeys(published.id(), news.id());
        assertThat(visible.get(published.id()).slug()).isEqualTo(Slug.of("publiee"));
    }

    @Test
    void asking_for_no_identifier_costs_no_query() {
        Statistics statistics = statistics();

        assertThat(publicationQueryService.visibleIds(Set.of())).isEmpty();
        assertThat(publicationQueryService.visibleById(Set.of())).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    @Test
    void finds_a_publication_by_slug_whatever_its_status() {
        assertThat(publicationQueryService.findBySlug(Slug.of("brouillon"))).map(Publication::id).contains(draft.id());
        assertThat(publicationQueryService.findBySlug(Slug.of("inconnue"))).isEmpty();
    }

    private Statistics statistics() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }
}
