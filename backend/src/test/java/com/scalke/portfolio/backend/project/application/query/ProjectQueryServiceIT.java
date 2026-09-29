package com.scalke.portfolio.backend.project.application.query;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.scalke.portfolio.backend.project.ProjectFixtures.project;
import static com.scalke.portfolio.backend.project.ProjectFixtures.published;
import static com.scalke.portfolio.backend.project.ProjectFixtures.technology;
import static com.scalke.portfolio.backend.project.ProjectFixtures.withText;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Façade de lecture pour les autres modules (D-CC) : seuls les projets {@code PUBLISHED} en sortent.
 */
@Transactional
class ProjectQueryServiceIT extends AbstractIntegrationTest {

    private static final LocalDate START = LocalDate.of(2025, 1, 1);

    @Autowired
    ProjectQueryService projectQueryService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    TechnologyRepository technologyRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    /**
     * Invariant 11 : un brouillon ou une archive n'est jamais trouvé, en une seule requête.
     */
    @Test
    void searches_only_the_published_projects() {
        long visible = create(published("publie", START, 0));
        create(project("brouillon", ProjectVisibility.DRAFT, DateRange.ongoingSince(START), 0));
        create(project("archive", ProjectVisibility.ARCHIVED, DateRange.ongoingSince(START), 0));
        Statistics statistics = statistics();

        Map<Long, Double> found = projectQueryService.searchPublished("projet");

        assertThat(found).containsOnlyKeys(visible);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    /**
     * Les technologies jouent pour un projet le rôle des tags : elles comptent comme le titre.
     */
    @Test
    void ranks_the_title_and_the_technologies_above_the_short_description_and_it_above_the_description() {
        Technology observability = technologyRepository.create(technology("Observabilité", "observabilite", 0));
        long inTitle = create(withText(published("dans-le-titre", START, 0),
            "L'observabilité en production", "Résumé", "Description"));
        long inTechnology = create(withText(published("dans-une-technologie", START, 0, observability),
            "Titre", "Résumé", "Description"));
        long inShortDescription = create(withText(published("dans-la-description-courte", START, 0),
            "Titre", "Mesurer l'observabilité", "Description"));
        long inDescription = create(withText(published("dans-la-description", START, 0),
            "Titre", "Résumé", "Parlons d'observabilité."));

        Map<Long, Double> ranks = projectQueryService.searchPublished("observabilite");

        assertThat(ranks).containsOnlyKeys(inTitle, inTechnology, inShortDescription, inDescription);
        assertThat(ranks.get(inTechnology)).isEqualTo(ranks.get(inTitle));
        assertThat(ranks.get(inTitle)).isGreaterThan(ranks.get(inShortDescription));
        assertThat(ranks.get(inShortDescription)).isGreaterThan(ranks.get(inDescription));
    }

    /**
     * La requête lit le texte avec la configuration du document ({@code french_unaccent}, D-BZ).
     */
    @Test
    void ignores_accents_and_case() {
        long api = create(withText(published("api", START, 0), "Développement d'une API", "Résumé", "Description"));

        assertThat(projectQueryService.searchPublished("développement")).containsOnlyKeys(api);
        assertThat(projectQueryService.searchPublished("DEVELOPPEMENT")).containsOnlyKeys(api);
    }

    @Test
    void loads_the_published_projects_among_those_requested() {
        Technology java = technologyRepository.create(technology("Java", "java", 0));
        long visible = create(published("publie", START, 0, java));
        long draft = create(project("brouillon", ProjectVisibility.DRAFT, DateRange.ongoingSince(START), 0));
        entityManager.clear();

        Map<Long, Project> found = projectQueryService.publishedById(List.of(visible, draft, 999_999L));

        assertThat(found).containsOnlyKeys(visible);
        assertThat(found.get(visible).technologies()).extracting(Technology::name).containsExactly("Java");
    }

    @Test
    void asking_for_no_identifier_costs_no_query() {
        Statistics statistics = statistics();

        assertThat(projectQueryService.publishedById(Set.of())).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isZero();
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
