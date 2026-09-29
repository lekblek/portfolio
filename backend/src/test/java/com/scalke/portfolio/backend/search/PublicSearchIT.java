package com.scalke.portfolio.backend.search;

import com.scalke.portfolio.backend.project.ProjectFixtures;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.domain.port.TagRepository;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static com.scalke.portfolio.backend.project.ProjectFixtures.project;
import static com.scalke.portfolio.backend.project.ProjectFixtures.published;
import static com.scalke.portfolio.backend.project.ProjectFixtures.technology;
import static com.scalke.portfolio.backend.publication.PublicationFixtures.article;
import static com.scalke.portfolio.backend.publication.PublicationFixtures.classifiedArticle;
import static com.scalke.portfolio.backend.publication.PublicationFixtures.publication;
import static com.scalke.portfolio.backend.publication.PublicationFixtures.withText;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours HTTP complet : contrôleur → cas d'usage de recherche → façades de {@code publication} et de
 * {@code project} → PostgreSQL (documents générés, déclencheurs, {@code ts_rank}). Vérifie la sérialisation
 * réelle, le classement commun et la visibilité (D-CC à D-CF).
 */
@Transactional
class PublicSearchIT extends AbstractIntegrationTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-06-10T08:30:00Z");
    private static final LocalDate START = LocalDate.of(2025, 1, 1);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PublicationRepository publicationRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    TechnologyRepository technologyRepository;

    @Autowired
    TagRepository tagRepository;

    @BeforeEach
    void givenPublicAndHiddenContent() {
        Tag observability = tagRepository.create(new Tag(null, "Observabilité", Slug.of("observabilite")));
        publicationRepository.create(withText(classifiedArticle("guide", PUBLISHED_AT, null, observability.id()),
            "Guide de production", "Traces, métriques et journaux", "Contenu"));
        publicationRepository.create(withText(publication("annonce", PublicationType.NEWS, PublicationStatus.PUBLISHED,
            PUBLISHED_AT), "Nouvelle version", "Ce qui change", "Une section sur l'observabilité."));
        projectRepository.create(ProjectFixtures.withText(
            published("plateforme", START, 0, technologyRepository.create(technology("Grafana", "grafana", 0))),
            "Plateforme", "Observabilité de bout en bout", "Description"));
        publicationRepository.create(withText(article("brouillon", PublicationStatus.DRAFT, null),
            "L'observabilité en brouillon", "Résumé", "Contenu"));
        projectRepository.create(ProjectFixtures.withText(
            project("projet-archive", ProjectVisibility.ARCHIVED, DateRange.ongoingSince(START), 0),
            "Observabilité archivée", "Résumé", "Description"));
    }

    /**
     * Tag (A) avant description courte (B) avant contenu (D) ; accents ignorés ; rien de caché.
     */
    @Test
    void finds_public_articles_news_and_projects_ranked_by_relevance() throws Exception {
        mockMvc.perform(get("/api/public/search").contextPath("/api").param("q", "OBSERVABILITE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(3))
            .andExpect(jsonPath("$.content[0].type").value("ARTICLE"))
            .andExpect(jsonPath("$.content[0].slug").value("guide"))
            .andExpect(jsonPath("$.content[0].summary").value("Traces, métriques et journaux"))
            .andExpect(jsonPath("$.content[0].publishedAt").value("2026-06-10T08:30:00Z"))
            .andExpect(jsonPath("$.content[1].type").value("PROJECT"))
            .andExpect(jsonPath("$.content[1].slug").value("plateforme"))
            .andExpect(jsonPath("$.content[1].publishedAt").value(nullValue()))
            .andExpect(jsonPath("$.content[2].type").value("NEWS"))
            .andExpect(jsonPath("$.content[2].slug").value("annonce"))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void finds_a_project_by_the_name_of_its_technology() throws Exception {
        mockMvc.perform(get("/api/public/search").contextPath("/api").param("q", "grafana"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].slug").value("plateforme"));
    }

    @Test
    void paginates_the_results() throws Exception {
        mockMvc.perform(get("/api/public/search").contextPath("/api")
                .param("q", "observabilité")
                .param("page", "1")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].slug").value("annonce"))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.first").value(false))
            .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void an_empty_text_finds_nothing_and_a_missing_one_is_rejected() throws Exception {
        mockMvc.perform(get("/api/public/search").contextPath("/api").param("q", "  "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/public/search").contextPath("/api"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
}
