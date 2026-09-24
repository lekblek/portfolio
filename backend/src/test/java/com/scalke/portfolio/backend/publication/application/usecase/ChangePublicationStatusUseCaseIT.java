package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Changement de statut de bout en bout (sans HTTP avant l'étape 36, D-AU) : domaine → port → PostgreSQL,
 * avec l'horloge fixe des tests.
 */
@Transactional
class ChangePublicationStatusUseCaseIT extends AbstractIntegrationTest {

    private static final Instant CREATED = NOW.minus(Duration.ofDays(2));

    @Autowired
    ChangePublicationStatusUseCase changePublicationStatusUseCase;

    @Autowired
    ListVisiblePublicationsUseCase listVisiblePublicationsUseCase;

    @Autowired
    PublicationRepository publicationRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    void persists_a_planned_publication() {
        Long id = create("brouillon", PublicationStatus.DRAFT, null);
        Instant scheduledAt = NOW.plus(Duration.ofDays(5));

        changePublicationStatusUseCase.execute(id, PublicationStatus.SCHEDULED, scheduledAt);

        Publication stored = reload(id);
        assertThat(stored.status()).isEqualTo(PublicationStatus.SCHEDULED);
        assertThat(stored.publishedAt()).isEqualTo(scheduledAt);
        assertThat(stored.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void publishing_makes_the_publication_visible_immediately() {
        Long id = create("a-publier", PublicationStatus.IN_REVIEW, null);

        changePublicationStatusUseCase.execute(id, PublicationStatus.PUBLISHED, null);
        entityManager.flush();
        entityManager.clear();

        assertThat(listVisiblePublicationsUseCase.execute(PublicationCriteria.none(), new PageQuery(0, 10)).content())
            .singleElement()
            .satisfies(visible -> {
                assertThat(visible.publication().slug()).isEqualTo("a-publier");
                assertThat(visible.publication().publishedAt()).isEqualTo(NOW);
            });
    }

    @Test
    void archiving_hides_the_publication_and_keeps_its_date() {
        Instant publishedAt = NOW.minus(Duration.ofDays(1));
        Long id = create("publiee", PublicationStatus.PUBLISHED, publishedAt);

        changePublicationStatusUseCase.execute(id, PublicationStatus.ARCHIVED, null);

        Publication stored = reload(id);
        assertThat(stored.status()).isEqualTo(PublicationStatus.ARCHIVED);
        assertThat(stored.publishedAt()).isEqualTo(publishedAt);
        assertThat(listVisiblePublicationsUseCase.execute(PublicationCriteria.none(), new PageQuery(0, 10)).content())
            .isEmpty();
    }

    @Test
    void a_refused_transition_changes_nothing() {
        Instant publishedAt = NOW.minus(Duration.ofDays(1));
        Long id = create("publiee", PublicationStatus.PUBLISHED, publishedAt);

        assertThatThrownBy(() -> changePublicationStatusUseCase.execute(id, PublicationStatus.DRAFT, null))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessage("Transition interdite : PUBLISHED → DRAFT.");

        Publication stored = reload(id);
        assertThat(stored.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(stored.updatedAt()).isEqualTo(CREATED);
    }

    @Test
    void fails_for_an_unknown_publication() {
        assertThatThrownBy(() -> changePublicationStatusUseCase.execute(999_999L, PublicationStatus.PUBLISHED, null))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Publication introuvable.");
    }

    /**
     * Publication créée avant « maintenant » : un changement de statut doit mettre {@code updatedAt} à {@code NOW}.
     */
    private Long create(String slug, PublicationStatus status, Instant publishedAt) {
        return publicationRepository.create(new Publication(null, PublicationType.ARTICLE, "Titre", slug, "Résumé",
            "Contenu", status, publishedAt, false, null, Set.of(), null, null, CREATED, CREATED)).id();
    }

    private Publication reload(Long id) {
        entityManager.flush();
        entityManager.clear();
        return publicationRepository.findById(id).orElseThrow();
    }
}
