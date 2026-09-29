package com.scalke.portfolio.backend.publication.application.query;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.domain.port.TagRepository;
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
import static com.scalke.portfolio.backend.publication.PublicationFixtures.classifiedArticle;
import static com.scalke.portfolio.backend.publication.PublicationFixtures.publication;
import static com.scalke.portfolio.backend.publication.PublicationFixtures.withText;
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
    TagRepository tagRepository;

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
    void finds_a_visible_publication_by_slug() {
        assertThat(publicationQueryService.findVisibleBySlug(Slug.of("planifiee-passee"))).map(Publication::id)
            .contains(due.id());
        assertThat(publicationQueryService.findVisibleBySlug(Slug.of("planifiee-future"))).isEmpty();
        assertThat(publicationQueryService.findVisibleBySlug(Slug.of("brouillon"))).isEmpty();
    }

    @Test
    void finds_a_publication_by_slug_whatever_its_status() {
        assertThat(publicationQueryService.findBySlug(Slug.of("brouillon"))).map(Publication::id).contains(draft.id());
        assertThat(publicationQueryService.findBySlug(Slug.of("inconnue"))).isEmpty();
    }

    // ---- Recherche plein texte (D-CC) ----

    /**
     * Toutes les publications contiennent « Contenu » ; seules les visibles sont trouvées, en deux requêtes
     * (recherche, puis règle de visibilité du module).
     */
    @Test
    void searches_only_the_visible_publications() {
        Statistics statistics = statistics();

        Map<Long, Double> found = publicationQueryService.searchVisible("contenu");

        assertThat(found).containsOnlyKeys(published.id(), due.id(), news.id());
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    /**
     * 01 §11 : titre et tags (forte), résumé (moyenne), contenu (normale). Un tag compte comme le titre.
     */
    @Test
    void ranks_the_title_and_the_tags_above_the_summary_and_the_summary_above_the_content() {
        Tag observability = tagRepository.create(new Tag(null, "Observabilité", Slug.of("observabilite")));
        long inTitle = create(withText(article("dans-le-titre", PublicationStatus.PUBLISHED, NOW),
            "L'observabilité en production", "Résumé", "Contenu"));
        long inTag = create(withText(classifiedArticle("dans-un-tag", NOW, null, observability.id()),
            "Titre", "Résumé", "Contenu"));
        long inSummary = create(withText(article("dans-le-resume", PublicationStatus.PUBLISHED, NOW),
            "Titre", "Mesurer l'observabilité", "Contenu"));
        long inContent = create(withText(article("dans-le-contenu", PublicationStatus.PUBLISHED, NOW),
            "Titre", "Résumé", "Parlons d'observabilité."));

        Map<Long, Double> ranks = publicationQueryService.searchVisible("observabilite");

        assertThat(ranks).containsOnlyKeys(inTitle, inTag, inSummary, inContent);
        assertThat(ranks.get(inTag)).isEqualTo(ranks.get(inTitle));
        assertThat(ranks.get(inTitle)).isGreaterThan(ranks.get(inSummary));
        assertThat(ranks.get(inSummary)).isGreaterThan(ranks.get(inContent));
    }

    /**
     * La requête lit le texte avec la configuration du document ({@code french_unaccent}, D-BZ).
     */
    @Test
    void ignores_accents_and_case_when_searching() {
        long api = create(withText(article("api", PublicationStatus.PUBLISHED, NOW),
            "Développement d'une API", "Résumé", "Contenu"));

        assertThat(publicationQueryService.searchVisible("développement")).containsOnlyKeys(api);
        assertThat(publicationQueryService.searchVisible("DEVELOPPEMENT")).containsOnlyKeys(api);
    }

    /**
     * Rien trouvé (mot absent, ou seulement des mots vides) : pas de seconde requête pour la visibilité.
     */
    @Test
    void a_search_that_finds_nothing_costs_one_query() {
        Statistics statistics = statistics();

        assertThat(publicationQueryService.searchVisible("introuvable")).isEmpty();
        assertThat(publicationQueryService.searchVisible("de la")).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    private long create(Publication publication) {
        long id = publicationRepository.create(publication).id();
        entityManager.flush();
        return id;
    }

    private Statistics statistics() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }
}
