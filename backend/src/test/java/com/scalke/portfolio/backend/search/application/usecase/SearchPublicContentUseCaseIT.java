package com.scalke.portfolio.backend.search.application.usecase;

import com.scalke.portfolio.backend.project.ProjectFixtures;
import com.scalke.portfolio.backend.project.application.query.ProjectQueryService;
import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.publication.application.query.PublicationQueryService;
import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.search.application.usecase.SearchResult.Type;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static com.scalke.portfolio.backend.project.ProjectFixtures.project;
import static com.scalke.portfolio.backend.project.ProjectFixtures.published;
import static com.scalke.portfolio.backend.project.ProjectFixtures.technology;
import static com.scalke.portfolio.backend.publication.PublicationFixtures.article;
import static com.scalke.portfolio.backend.publication.PublicationFixtures.publication;
import static com.scalke.portfolio.backend.publication.PublicationFixtures.withText;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Recherche publique de bout en bout, sans HTTP (route à l'étape 29) : les modules propriétaires cherchent et
 * appliquent leur visibilité, le cas d'usage classe l'ensemble et pagine (D-CD).
 */
@Transactional
class SearchPublicContentUseCaseIT extends AbstractIntegrationTest {

    private static final Instant YESTERDAY = NOW.minus(Duration.ofDays(1));
    private static final LocalDate START = LocalDate.of(2025, 1, 1);
    private static final PageQuery FIRST_PAGE = new PageQuery(0, 10);

    @Autowired
    SearchPublicContentUseCase searchPublicContentUseCase;

    @Autowired
    PublicationQueryService publicationQueryService;

    @Autowired
    ProjectQueryService projectQueryService;

    @Autowired
    PublicationRepository publicationRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    TechnologyRepository technologyRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    /**
     * Un seul classement pour les trois genres de contenus : titre, puis description courte, puis contenu.
     */
    @Test
    void ranks_articles_news_and_projects_together() {
        create(withText(publication("annonce", PublicationType.NEWS, PublicationStatus.PUBLISHED, YESTERDAY),
            "Nouvelle version", "Ce qui change", "Une section sur l'observabilité."));
        create(withText(article("guide", PublicationStatus.PUBLISHED, YESTERDAY),
            "Guide de l'observabilité", "Traces, métriques et journaux", "Contenu"));
        create(ProjectFixtures.withText(published("plateforme", START, 0),
            "Plateforme", "Observabilité de bout en bout", "Description"));

        PageResult<SearchResult> page = search("observabilite", FIRST_PAGE);

        assertThat(page.content())
            .extracting(SearchResult::type, SearchResult::slug, SearchResult::summary, SearchResult::publishedAt)
            .containsExactly(
                tuple(Type.ARTICLE, Slug.of("guide"), "Traces, métriques et journaux", YESTERDAY),
                tuple(Type.PROJECT, Slug.of("plateforme"), "Observabilité de bout en bout", null),
                tuple(Type.NEWS, Slug.of("annonce"), "Ce qui change", YESTERDAY));
        assertThat(page.totalElements()).isEqualTo(3);
    }

    /**
     * Invariants 7 à 11 : la recherche ne révèle rien de ce que les lectures publiques cachent. Tous les
     * contenus ci-dessous ont un résumé ; seule la publication planifiée dont la date est passée est publique.
     */
    @Test
    void never_finds_content_that_is_not_public() {
        create(article("brouillon", PublicationStatus.DRAFT, null));
        create(article("en-relecture", PublicationStatus.IN_REVIEW, null));
        create(article("planifiee", PublicationStatus.SCHEDULED, NOW.plusSeconds(1)));
        create(article("archivee", PublicationStatus.ARCHIVED, YESTERDAY));
        create(project("projet-brouillon", ProjectVisibility.DRAFT, DateRange.ongoingSince(START), 0));
        create(project("projet-archive", ProjectVisibility.ARCHIVED, DateRange.ongoingSince(START), 0));
        create(article("planifiee-echue", PublicationStatus.SCHEDULED, NOW.minusSeconds(1)));

        PageResult<SearchResult> page = search("résumé", FIRST_PAGE);

        assertThat(page.content()).extracting(SearchResult::slug).containsExactly(Slug.of("planifiee-echue"));
        assertThat(page.totalElements()).isEqualTo(1);
    }

    /**
     * Les deux modules calculent la pertinence avec la même configuration, les mêmes poids et la même formule :
     * un article et un projet de même texte sont aussi pertinents, et l'article passe devant (ordre total).
     */
    @Test
    void a_publication_and_a_project_with_the_same_text_are_equally_relevant() {
        long publication = create(withText(article("jumeau", PublicationStatus.PUBLISHED, YESTERDAY),
            "Déployer sur Kubernetes", "Un cluster à la maison", "Avec Helm et Argo CD."));
        long project = create(ProjectFixtures.withText(published("jumeau", START, 0),
            "Déployer sur Kubernetes", "Un cluster à la maison", "Avec Helm et Argo CD."));

        double publicationRank = publicationQueryService.searchVisible("kubernetes helm").get(publication);
        double projectRank = projectQueryService.searchPublished("kubernetes helm").get(project);

        assertThat(publicationRank).isPositive().isEqualTo(projectRank);
        assertThat(search("kubernetes helm", FIRST_PAGE).content()).extracting(SearchResult::type)
            .containsExactly(Type.ARTICLE, Type.PROJECT);
    }

    @Test
    void ignores_accents_and_finds_a_project_by_its_technologies() {
        create(withText(article("api", PublicationStatus.PUBLISHED, YESTERDAY),
            "Développement d'une API", "Résumé", "Contenu"));
        create(published("application", START, 0, technologyRepository.create(technology("PostgreSQL", "postgresql", 0))));

        assertThat(search("developpement", FIRST_PAGE).content()).extracting(SearchResult::slug)
            .containsExactly(Slug.of("api"));
        assertThat(search("postgresql", FIRST_PAGE).content()).extracting(SearchResult::slug)
            .containsExactly(Slug.of("application"));
    }

    @Test
    void paginates_the_ranked_results_and_counts_them_all() {
        for (int i = 1; i <= 3; i++) {
            create(article("article-" + i, PublicationStatus.PUBLISHED, YESTERDAY));
            create(published("projet-" + i, START, 0));
        }

        PageResult<SearchResult> first = search("titre or projet", new PageQuery(0, 4));
        PageResult<SearchResult> last = search("titre or projet", new PageQuery(1, 4));

        assertThat(first.totalElements()).isEqualTo(6);
        assertThat(first.content()).hasSize(4);
        assertThat(last.content()).hasSize(2);
        assertThat(last.content()).doesNotContainAnyElementsOf(first.content());
    }

    /**
     * Coût constant : recherche des publications et règle de visibilité, recherche des projets, puis la page
     * (publications et leurs tags, projets, leurs technologies et leurs captures), quel que soit le nombre de
     * résultats.
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 4})
    void costs_a_constant_number_of_queries(int perKind) {
        for (int i = 1; i <= perKind; i++) {
            create(article("article-" + i, PublicationStatus.PUBLISHED, YESTERDAY));
            create(published("projet-" + i, START, 0));
        }
        entityManager.clear();
        Statistics statistics = statistics();

        PageResult<SearchResult> page = search("titre or projet", FIRST_PAGE);

        assertThat(page.content()).hasSize(2 * perKind);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(8);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void a_blank_text_finds_nothing_without_any_query(String text) {
        Statistics statistics = statistics();

        PageResult<SearchResult> page = search(text, FIRST_PAGE);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    /**
     * Une saisie faite seulement de mots vides ou de ponctuation ne trouve rien, sans erreur.
     */
    @ParameterizedTest
    @ValueSource(strings = {"de la", "(((&|!:*", "\"\""})
    void a_text_without_any_searchable_word_finds_nothing(String text) {
        create(article("publiee", PublicationStatus.PUBLISHED, YESTERDAY));

        assertThat(search(text, FIRST_PAGE).content()).isEmpty();
    }

    private PageResult<SearchResult> search(String text, PageQuery query) {
        return searchPublicContentUseCase.execute(text, query);
    }

    private long create(Publication publication) {
        long id = publicationRepository.create(publication).id();
        entityManager.flush();
        return id;
    }

    private long create(Project project) {
        long id = projectRepository.create(project).id();
        entityManager.flush();
        return id;
    }

    private Statistics statistics() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }
}
