package com.scalke.portfolio.backend.series;

import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static com.scalke.portfolio.backend.publication.PublicationFixtures.article;
import static com.scalke.portfolio.backend.series.SeriesFixtures.series;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours HTTP complet : contrôleur → cas d'usage → adaptateur JPA des séries et façade du module
 * {@code publication} (horloge fixe) → PostgreSQL.
 */
@Transactional
class PublicSeriesIT extends AbstractIntegrationTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-06-10T08:30:00Z");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    SeriesRepository seriesRepository;

    @Autowired
    PublicationRepository publicationRepository;

    @BeforeEach
    void givenAVisibleAndAnInvisibleSeries() {
        seriesRepository.create(series("Spring Boot de zéro à la production", "spring-boot-de-zero-a-la-production",
            create("installer-spring-boot", PublicationStatus.PUBLISHED, PUBLISHED_AT),
            create("chapitre-a-venir", PublicationStatus.SCHEDULED, NOW.plusSeconds(60)),
            create("securiser-l-api", PublicationStatus.SCHEDULED, NOW.minusSeconds(60))));
        seriesRepository.create(series("angular-moderne", create("brouillon", PublicationStatus.DRAFT, null)));
    }

    @Test
    void lists_visible_series_with_their_number_of_visible_chapters() throws Exception {
        mockMvc.perform(get("/api/public/series").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].slug").value("spring-boot-de-zero-a-la-production"))
            .andExpect(jsonPath("$.content[0].chapterCount").value(2))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void returns_a_series_with_its_visible_chapters() throws Exception {
        mockMvc.perform(get("/api/public/series/spring-boot-de-zero-a-la-production").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Spring Boot de zéro à la production"))
            .andExpect(jsonPath("$.chapters.length()").value(2))
            .andExpect(jsonPath("$.chapters[0].position").value(1))
            .andExpect(jsonPath("$.chapters[0].slug").value("installer-spring-boot"))
            .andExpect(jsonPath("$.chapters[0].publishedAt").value("2026-06-10T08:30:00Z"))
            .andExpect(jsonPath("$.chapters[1].position").value(2))
            .andExpect(jsonPath("$.chapters[1].slug").value("securiser-l-api"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"angular-moderne", "inconnue"})
    void hides_invisible_series_behind_a_404(String slug) throws Exception {
        mockMvc.perform(get("/api/public/series/" + slug).contextPath("/api"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.detail").value("Série introuvable."));
    }

    private Long create(String slug, PublicationStatus status, Instant publishedAt) {
        return publicationRepository.create(article(slug, status, publishedAt)).id();
    }
}
