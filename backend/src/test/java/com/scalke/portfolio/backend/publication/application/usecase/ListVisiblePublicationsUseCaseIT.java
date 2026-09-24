package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.domain.port.CategoryRepository;
import com.scalke.portfolio.backend.taxonomy.domain.port.TagRepository;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.scalke.portfolio.backend.publication.PublicationFixtures.article;
import static com.scalke.portfolio.backend.publication.PublicationFixtures.classifiedArticle;
import static com.scalke.portfolio.backend.publication.PublicationFixtures.publication;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ListVisiblePublicationsUseCaseIT extends AbstractIntegrationTest {

    @Autowired
    ListVisiblePublicationsUseCase listVisiblePublicationsUseCase;

    @Autowired
    PublicationRepository publicationRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private Category backend;
    private Tag java;
    private Tag tests;

    // ---- Visibilité (invariants 7 à 10, D-AH), évaluée avec l'horloge fixe des tests ----

    /**
     * PUBLISHED et SCHEDULED échue (y compris à l'instant exact) sont visibles ; tout le reste ne l'est pas.
     * Ordre : les plus récentes d'abord.
     */
    @Test
    void returns_only_visible_publications_most_recent_first() {
        givenOnePublicationPerVisibilityCase();

        PageResult<VisiblePublication> page = list(PublicationCriteria.none(), new PageQuery(0, 10));

        assertThat(page.content())
            .extracting(ListVisiblePublicationsUseCaseIT::slug)
            .containsExactly("planifiee-maintenant", "planifiee-passee", "news-publiee", "publiee-ancienne");
        assertThat(page.totalElements()).isEqualTo(4);
    }

    /**
     * D-AY : la règle du domaine ({@code Publication.isVisibleAt}) et celle de la requête
     * ({@code PublicationSpecifications.visibleAt}) désignent exactement les mêmes publications.
     */
    @Test
    void the_domain_rule_and_the_query_agree_on_visibility() {
        List<Publication> all = givenOnePublicationPerVisibilityCase();

        Set<String> visibleByDomain = all.stream()
            .filter(publication -> publication.isVisibleAt(NOW))
            .map(Publication::slug)
            .collect(Collectors.toSet());
        Set<String> visibleByQuery = list(PublicationCriteria.none(), new PageQuery(0, 100)).content().stream()
            .map(ListVisiblePublicationsUseCaseIT::slug)
            .collect(Collectors.toSet());

        assertThat(visibleByQuery).hasSize(4).isEqualTo(visibleByDomain);
    }

    @Test
    void filters_by_type() {
        givenOnePublicationPerVisibilityCase();

        PageResult<VisiblePublication> news = list(
            PublicationCriteria.of(PublicationType.NEWS, null, null), new PageQuery(0, 10));

        assertThat(news.content()).extracting(ListVisiblePublicationsUseCaseIT::slug).containsExactly("news-publiee");
        assertThat(news.totalElements()).isEqualTo(1);
    }

    @Test
    void paginates_visible_publications_only() {
        givenOnePublicationPerVisibilityCase();

        PageResult<VisiblePublication> page = list(PublicationCriteria.none(), new PageQuery(1, 3));

        assertThat(page.content()).extracting(ListVisiblePublicationsUseCaseIT::slug).containsExactly("publiee-ancienne");
        assertThat(page.totalElements()).isEqualTo(4);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    // ---- Classement (D-AN, D-AP, D-AQ) : termes résolus par la façade de la taxonomie ----

    @Test
    void returns_each_publication_with_its_category_and_tags_sorted_by_name() {
        givenClassifiedPublications();

        PageResult<VisiblePublication> page = list(PublicationCriteria.none(), new PageQuery(0, 10));

        VisiblePublication api = page.content().getFirst();
        assertThat(api.category()).isEqualTo(backend);
        assertThat(api.tags()).containsExactly(java, tests);
        VisiblePublication unclassified = page.content().getLast();
        assertThat(unclassified.category()).isNull();
        assertThat(unclassified.tags()).isEmpty();
    }

    @Test
    void filters_by_category_slug() {
        givenClassifiedPublications();

        PageResult<VisiblePublication> page = list(PublicationCriteria.of(null, "frontend", null), new PageQuery(0, 10));

        assertThat(page.content()).extracting(ListVisiblePublicationsUseCaseIT::slug).containsExactly("ssr-angular");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    /**
     * Le filtre restreint les publications, pas la liste de leurs tags ; le comptage reste exact.
     */
    @Test
    void filters_by_tag_slug_without_duplicates() {
        givenClassifiedPublications();

        PageResult<VisiblePublication> page = list(PublicationCriteria.of(null, null, "tests"), new PageQuery(0, 10));

        assertThat(page.content()).extracting(ListVisiblePublicationsUseCaseIT::slug)
            .containsExactly("api-spring", "ssr-angular");
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content().getFirst().tags()).containsExactly(java, tests);
    }

    @Test
    void combines_filters() {
        givenClassifiedPublications();

        PageResult<VisiblePublication> page = list(
            PublicationCriteria.of(PublicationType.ARTICLE, "backend", "tests"), new PageQuery(0, 10));

        assertThat(page.content()).extracting(ListVisiblePublicationsUseCaseIT::slug).containsExactly("api-spring");
    }

    @Test
    void an_unknown_category_or_tag_gives_an_empty_page() {
        givenClassifiedPublications();

        assertThat(list(PublicationCriteria.of(null, "inconnue", null), new PageQuery(0, 10)).content()).isEmpty();
        assertThat(list(PublicationCriteria.of(null, null, "inconnu"), new PageQuery(0, 10)).totalElements()).isZero();
    }

    /**
     * D-AS : une page complète coûte 5 requêtes (publications, comptage, tags de la page, catégories,
     * termes des tags), quel que soit le nombre de publications. Sans {@code @BatchSize}, il y aurait
     * une requête de tags par publication.
     */
    @Test
    void loads_a_page_and_its_terms_in_a_constant_number_of_queries() {
        givenClassifiedPublications();
        for (int i = 0; i < 3; i++) {
            publicationRepository.create(classifiedArticle("autre-" + i, NOW.minus(Duration.ofDays(10 + i)),
                backend.id(), java.id()));
        }
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        PageResult<VisiblePublication> page = list(PublicationCriteria.none(), new PageQuery(0, 4));

        assertThat(page.content()).hasSize(4);
        assertThat(page.totalElements()).isEqualTo(6);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(5);
    }

    private PageResult<VisiblePublication> list(PublicationCriteria criteria, PageQuery query) {
        return listVisiblePublicationsUseCase.execute(criteria, query);
    }

    private static String slug(VisiblePublication visible) {
        return visible.publication().slug();
    }

    private List<Publication> givenOnePublicationPerVisibilityCase() {
        return List.of(
            article("publiee-ancienne", PublicationStatus.PUBLISHED, NOW.minus(Duration.ofDays(30))),
            publication("news-publiee", PublicationType.NEWS, PublicationStatus.PUBLISHED, NOW.minus(Duration.ofDays(3))),
            article("planifiee-passee", PublicationStatus.SCHEDULED, NOW.minus(Duration.ofDays(1))),
            article("planifiee-maintenant", PublicationStatus.SCHEDULED, NOW),
            article("planifiee-future", PublicationStatus.SCHEDULED, NOW.plusSeconds(1)),
            article("brouillon", PublicationStatus.DRAFT, null),
            article("en-relecture", PublicationStatus.IN_REVIEW, null),
            article("archivee", PublicationStatus.ARCHIVED, NOW.minus(Duration.ofDays(60)))
        ).stream().map(publicationRepository::create).toList();
    }

    /**
     * Deux articles classés (backend : java, tests ; frontend : angular, tests), un non classé, un brouillon.
     */
    private void givenClassifiedPublications() {
        backend = categoryRepository.create(new Category(null, "Backend", "backend", null));
        Category frontend = categoryRepository.create(new Category(null, "Frontend", "frontend", null));
        java = tagRepository.create(new Tag(null, "Java", "java"));
        Tag angular = tagRepository.create(new Tag(null, "Angular", "angular"));
        tests = tagRepository.create(new Tag(null, "Tests", "tests"));
        publicationRepository.create(classifiedArticle("api-spring", NOW.minus(Duration.ofDays(1)),
            backend.id(), java.id(), tests.id()));
        publicationRepository.create(classifiedArticle("ssr-angular", NOW.minus(Duration.ofDays(2)),
            frontend.id(), angular.id(), tests.id()));
        publicationRepository.create(classifiedArticle("sans-classement", NOW.minus(Duration.ofDays(3)), null));
        publicationRepository.create(article("brouillon-backend", PublicationStatus.DRAFT, null));
        entityManager.flush();
        entityManager.clear();
    }
}
