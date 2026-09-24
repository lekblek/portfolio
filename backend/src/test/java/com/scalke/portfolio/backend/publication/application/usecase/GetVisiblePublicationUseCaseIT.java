package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static com.scalke.portfolio.backend.publication.PublicationFixtures.article;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class GetVisiblePublicationUseCaseIT extends AbstractIntegrationTest {

    @Autowired
    GetVisiblePublicationUseCase getVisiblePublicationUseCase;

    @Autowired
    PublicationRepository publicationRepository;

    private Publication published;

    @BeforeEach
    void givenPublicationsInEveryStatus() {
        published = publicationRepository.create(new Publication(
            null, PublicationType.ARTICLE, "Construire une API", "construire-une-api", "Résumé", "## Contenu",
            PublicationStatus.PUBLISHED, NOW.minus(Duration.ofDays(2)), true, "Titre SEO", null,
            NOW.minus(Duration.ofDays(3)), NOW.minus(Duration.ofDays(2))));
        publicationRepository.create(article("planifiee-passee", PublicationStatus.SCHEDULED, NOW.minusSeconds(1)));
        publicationRepository.create(article("planifiee-future", PublicationStatus.SCHEDULED, NOW.plus(Duration.ofDays(1))));
        publicationRepository.create(article("brouillon", PublicationStatus.DRAFT, null));
        publicationRepository.create(article("en-relecture", PublicationStatus.IN_REVIEW, null));
        publicationRepository.create(article("archivee", PublicationStatus.ARCHIVED, NOW.minus(Duration.ofDays(60))));
    }

    @Test
    void returns_a_published_publication_with_every_field() {
        Publication publication = getVisiblePublicationUseCase.execute("construire-une-api");

        assertThat(publication).isEqualTo(published);
        assertThat(publication.seoDescription()).isNull();
    }

    @Test
    void returns_a_scheduled_publication_once_its_date_has_passed() {
        Instant publishedAt = getVisiblePublicationUseCase.execute("planifiee-passee").publishedAt();

        assertThat(publishedAt).isBefore(NOW);
    }

    /**
     * Invisible et inexistant sont indiscernables (05 §9 et §29).
     */
    @ParameterizedTest
    @ValueSource(strings = {"planifiee-future", "brouillon", "en-relecture", "archivee", "inconnue"})
    void fails_for_a_publication_that_is_not_visible(String slug) {
        assertThatThrownBy(() -> getVisiblePublicationUseCase.execute(slug))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Publication introuvable.");
    }
}
