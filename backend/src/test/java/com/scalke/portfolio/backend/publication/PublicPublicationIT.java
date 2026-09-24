package com.scalke.portfolio.backend.publication;

import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static com.scalke.portfolio.backend.publication.PublicationFixtures.article;
import static com.scalke.portfolio.backend.publication.PublicationFixtures.publication;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours HTTP complet : contrôleur → cas d'usage (horloge fixe) → adaptateur JPA → PostgreSQL.
 * Vérifie la sérialisation réelle des instants et la visibilité publique (D-AH).
 */
@Transactional
class PublicPublicationIT extends AbstractIntegrationTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-06-10T08:30:00Z");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PublicationRepository publicationRepository;

    @BeforeEach
    void givenVisibleAndInvisiblePublications() {
        publicationRepository.create(article("article-publie", PublicationStatus.PUBLISHED, PUBLISHED_AT));
        publicationRepository.create(publication("news-planifiee-passee", PublicationType.NEWS,
            PublicationStatus.SCHEDULED, NOW.minus(Duration.ofHours(1))));
        publicationRepository.create(article("article-planifie-futur", PublicationStatus.SCHEDULED,
            NOW.plus(Duration.ofHours(1))));
        publicationRepository.create(article("brouillon", PublicationStatus.DRAFT, null));
    }

    @Test
    void lists_visible_publications_with_iso_instants() throws Exception {
        mockMvc.perform(get("/api/public/publications").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].slug").value("news-planifiee-passee"))
            .andExpect(jsonPath("$.content[1].slug").value("article-publie"))
            .andExpect(jsonPath("$.content[1].publishedAt").value("2026-06-10T08:30:00Z"))
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void filters_by_type() throws Exception {
        mockMvc.perform(get("/api/public/publications").contextPath("/api").param("type", "ARTICLE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].slug").value("article-publie"));
    }

    @Test
    void returns_a_visible_publication_by_slug() throws Exception {
        mockMvc.perform(get("/api/public/publications/article-publie").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("ARTICLE"))
            .andExpect(jsonPath("$.contentMarkdown").value("# article-publie\n\nContenu."))
            .andExpect(jsonPath("$.readingTimeMinutes").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"article-planifie-futur", "brouillon", "inconnue"})
    void hides_invisible_publications_behind_a_404(String slug) throws Exception {
        mockMvc.perform(get("/api/public/publications/" + slug).contextPath("/api"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.detail").value("Publication introuvable."));
    }
}
