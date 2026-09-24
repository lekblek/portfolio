package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationFilter;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static com.scalke.portfolio.backend.publication.PublicationFixtures.article;
import static com.scalke.portfolio.backend.publication.PublicationFixtures.publication;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Règle de visibilité publique (invariants 7 à 10, D-AH), évaluée avec l'horloge fixe des tests.
 */
@Transactional
class ListVisiblePublicationsUseCaseIT extends AbstractIntegrationTest {

    private static final Instant ONE_DAY_AGO = NOW.minus(Duration.ofDays(1));

    @Autowired
    ListVisiblePublicationsUseCase listVisiblePublicationsUseCase;

    @Autowired
    PublicationRepository publicationRepository;

    @BeforeEach
    void givenOnePublicationPerVisibilityCase() {
        publicationRepository.create(article("publiee-ancienne", PublicationStatus.PUBLISHED, NOW.minus(Duration.ofDays(30))));
        publicationRepository.create(publication("news-publiee", PublicationType.NEWS, PublicationStatus.PUBLISHED,
            NOW.minus(Duration.ofDays(3))));
        publicationRepository.create(article("planifiee-passee", PublicationStatus.SCHEDULED, ONE_DAY_AGO));
        publicationRepository.create(article("planifiee-maintenant", PublicationStatus.SCHEDULED, NOW));
        publicationRepository.create(article("planifiee-future", PublicationStatus.SCHEDULED, NOW.plusSeconds(1)));
        publicationRepository.create(article("brouillon", PublicationStatus.DRAFT, null));
        publicationRepository.create(article("en-relecture", PublicationStatus.IN_REVIEW, null));
        publicationRepository.create(article("archivee", PublicationStatus.ARCHIVED, NOW.minus(Duration.ofDays(60))));
    }

    /**
     * PUBLISHED et SCHEDULED échue (y compris à l'instant exact) sont visibles ; tout le reste ne l'est pas.
     * Ordre : les plus récentes d'abord.
     */
    @Test
    void returns_only_visible_publications_most_recent_first() {
        PageResult<Publication> page = listVisiblePublicationsUseCase.execute(PublicationFilter.none(), new PageQuery(0, 10));

        assertThat(page.content())
            .extracting(Publication::slug)
            .containsExactly("planifiee-maintenant", "planifiee-passee", "news-publiee", "publiee-ancienne");
        assertThat(page.totalElements()).isEqualTo(4);
    }

    @Test
    void filters_by_type() {
        PageResult<Publication> news = listVisiblePublicationsUseCase.execute(
            PublicationFilter.ofType(PublicationType.NEWS), new PageQuery(0, 10));

        assertThat(news.content()).extracting(Publication::slug).containsExactly("news-publiee");
        assertThat(news.totalElements()).isEqualTo(1);
    }

    @Test
    void paginates_visible_publications_only() {
        PageResult<Publication> page = listVisiblePublicationsUseCase.execute(PublicationFilter.none(), new PageQuery(1, 3));

        assertThat(page.content()).extracting(Publication::slug).containsExactly("publiee-ancienne");
        assertThat(page.totalElements()).isEqualTo(4);
        assertThat(page.totalPages()).isEqualTo(2);
    }
}
