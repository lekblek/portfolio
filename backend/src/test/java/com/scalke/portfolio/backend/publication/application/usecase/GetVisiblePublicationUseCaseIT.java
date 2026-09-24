package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.domain.port.CategoryRepository;
import com.scalke.portfolio.backend.taxonomy.domain.port.TagRepository;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

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

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    EntityManager entityManager;

    private Publication published;
    private Category backend;
    private Tag java;
    private Tag springBoot;

    @BeforeEach
    void givenPublicationsInEveryStatus() {
        backend = categoryRepository.create(new Category(null, "Backend", "backend", "Spring Boot"));
        springBoot = tagRepository.create(new Tag(null, "Spring Boot", "spring-boot"));
        java = tagRepository.create(new Tag(null, "Java", "java"));
        published = publicationRepository.create(new Publication(
            null, PublicationType.ARTICLE, "Construire une API", "construire-une-api", "Résumé", "## Contenu",
            PublicationStatus.PUBLISHED, NOW.minus(Duration.ofDays(2)), true, backend.id(),
            Set.of(springBoot.id(), java.id()), "Titre SEO", null,
            NOW.minus(Duration.ofDays(3)), NOW.minus(Duration.ofDays(2))));
        publicationRepository.create(article("planifiee-passee", PublicationStatus.SCHEDULED, NOW.minusSeconds(1)));
        publicationRepository.create(article("planifiee-future", PublicationStatus.SCHEDULED, NOW.plus(Duration.ofDays(1))));
        publicationRepository.create(article("brouillon", PublicationStatus.DRAFT, null));
        publicationRepository.create(article("en-relecture", PublicationStatus.IN_REVIEW, null));
        publicationRepository.create(article("archivee", PublicationStatus.ARCHIVED, NOW.minus(Duration.ofDays(60))));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void returns_a_published_publication_with_every_field_and_its_terms() {
        VisiblePublication visible = getVisiblePublicationUseCase.execute("construire-une-api");

        assertThat(visible.publication()).isEqualTo(published);
        assertThat(visible.category()).isEqualTo(backend);
        assertThat(visible.tags()).containsExactly(java, springBoot);
    }

    @Test
    void returns_a_scheduled_publication_once_its_date_has_passed() {
        Instant publishedAt = getVisiblePublicationUseCase.execute("planifiee-passee").publication().publishedAt();

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
