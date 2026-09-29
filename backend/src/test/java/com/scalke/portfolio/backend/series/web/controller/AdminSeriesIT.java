package com.scalke.portfolio.backend.series.web.controller;

import com.scalke.portfolio.backend.media.MediaFixtures;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import com.scalke.portfolio.backend.publication.PublicationFixtures;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.InvalidInputException;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static com.scalke.portfolio.backend.publication.PublicationFixtures.publication;
import static com.scalke.portfolio.backend.series.SeriesFixtures.series;
import static com.scalke.portfolio.backend.series.SeriesFixtures.withCover;
import static com.scalke.portfolio.backend.testsupport.CsrfTestSupport.xsrf;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administration des séries de bout en bout (D-CV) : saisie, slug généré, suffixé ou verrouillé, couverture vérifiée,
 * chapitres remplacés d'un bloc (invariants 1 à 3), effet sur le site public.
 */
@Transactional
class AdminSeriesIT extends AbstractIntegrationTest {

    private static final Instant PUBLISHED_AT = NOW.minusSeconds(3_600);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    SeriesRepository seriesRepository;

    @Autowired
    PublicationRepository publicationRepository;

    @Autowired
    MediaRepository mediaRepository;

    @Test
    void requires_the_administrator_session_and_the_csrf_token() throws Exception {
        mockMvc.perform(get("/api/admin/series").contextPath("/api"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/series").contextPath("/api").with(user("admin"))
                .contentType(MediaType.APPLICATION_JSON).content(seriesJson("Angular")))
            .andExpect(status().isForbidden());
    }

    @Test
    void creates_a_series_without_chapters() throws Exception {
        long cover = mediaRepository.create(MediaFixtures.image("Couverture")).id();

        create("""
            {"title":" Spring Boot de zéro ","descriptionMarkdown":"# Série","coverMediaId":%d}
            """.formatted(cover))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", endsWith("/api/admin/series/" + idOf("spring-boot-de-zero"))))
            .andExpect(jsonPath("$.title").value("Spring Boot de zéro"))
            .andExpect(jsonPath("$.slug").value("spring-boot-de-zero"))
            .andExpect(jsonPath("$.slugLocked").value(false))
            .andExpect(jsonPath("$.coverMediaId").value(cover))
            .andExpect(jsonPath("$.chapters.length()").value(0));
        create(seriesJson("Spring Boot de zéro")).andExpect(jsonPath("$.slug").value("spring-boot-de-zero-2"));
    }

    @Test
    void validates_the_series() throws Exception {
        long pdf = mediaRepository.create(MediaFixtures.pdf()).id();

        create("{\"title\":\"!!!\",\"descriptionMarkdown\":\"\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("title"));
        create("{\"title\":\"Titre\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("descriptionMarkdown"));
        create("{\"title\":\"Titre\",\"descriptionMarkdown\":\"\",\"coverMediaId\":" + pdf + "}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("coverMediaId"));
    }

    /**
     * Les chapitres sont rangés dans l'ordre donné, puis réordonnés d'un bloc sans heurter la clé
     * {@code (series_id, position)} ; la série apparaît sur le site dès qu'un de ses articles est visible.
     */
    @Test
    void orders_and_reorders_the_chapters() throws Exception {
        long series = seriesRepository.create(series("angular")).id();
        long first = createArticle("premier", PublicationStatus.PUBLISHED, PUBLISHED_AT);
        long draft = createArticle("brouillon", PublicationStatus.DRAFT, null);

        changeChapters(series, List.of(draft, first))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.chapters[0].position").value(1))
            .andExpect(jsonPath("$.chapters[0].publicationId").value(draft))
            .andExpect(jsonPath("$.chapters[0].status").value("DRAFT"))
            .andExpect(jsonPath("$.chapters[1].position").value(2))
            .andExpect(jsonPath("$.chapters[1].slug").value("premier"))
            .andExpect(jsonPath("$.chapters[1].title").value("Titre premier"))
            .andExpect(jsonPath("$.slugLocked").value(true));
        changeChapters(series, List.of(first, draft))
            .andExpect(jsonPath("$.chapters[0].publicationId").value(first))
            .andExpect(jsonPath("$.chapters[1].publicationId").value(draft));

        mockMvc.perform(get("/api/public/series/angular").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.chapters.length()").value(1));
        changeChapters(series, List.of()).andExpect(jsonPath("$.chapters.length()").value(0));
        mockMvc.perform(get("/api/public/series/angular").contextPath("/api"))
            .andExpect(status().isNotFound());
    }

    /**
     * Invariants 1 et 2, et saisie invalide : une actualité, un article d'une autre série, une publication inconnue ou
     * répétée.
     */
    @Test
    void refuses_invalid_chapters() throws Exception {
        long series = seriesRepository.create(series("angular")).id();
        long news = publicationRepository.create(publication("breve", PublicationType.NEWS, PublicationStatus.DRAFT,
            null)).id();
        long taken = createArticle("deja-range", PublicationStatus.DRAFT, null);
        seriesRepository.create(series("autre", taken));
        long free = createArticle("libre", PublicationStatus.DRAFT, null);

        changeChapters(series, List.of(free, news))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("NEWS_CANNOT_JOIN_SERIES"));
        changeChapters(series, List.of(taken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ARTICLE_ALREADY_IN_SERIES"));
        changeChapters(series, List.of(999_999L))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("publicationIds"));
        changeChapters(series, List.of(free, free))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("publicationIds"));
        mockMvc.perform(put("/api/admin/series/" + series + "/chapters").contextPath("/api").with(user("admin"))
                .with(xsrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("publicationIds"));
        assertThat(seriesRepository.findById(series).orElseThrow().items()).isEmpty();
    }

    /**
     * D-BK : le slug d'une série ne change plus dès qu'un de ses articles a été public ; sa saisie, si.
     */
    @Test
    void updates_a_series_and_locks_its_slug_once_public() throws Exception {
        long draftSeries = seriesRepository.create(series("brouillon", createArticle("a", PublicationStatus.DRAFT, null)))
            .id();
        long publicSeries = seriesRepository.create(series("publique",
            createArticle("b", PublicationStatus.PUBLISHED, PUBLISHED_AT))).id();

        update(draftSeries, "{\"title\":\"Brouillon\",\"slug\":\"nouveau\",\"descriptionMarkdown\":\"\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("nouveau"))
            .andExpect(jsonPath("$.chapters.length()").value(1));
        update(publicSeries, "{\"title\":\"Publique\",\"slug\":\"autre\",\"descriptionMarkdown\":\"\"}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SLUG_LOCKED"));
        update(publicSeries, "{\"title\":\"Publique relue\",\"descriptionMarkdown\":\"Texte\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("publique"))
            .andExpect(jsonPath("$.title").value("Publique relue"));
    }

    @Test
    void lists_every_series_by_title() throws Exception {
        seriesRepository.create(series("Vue", "vue"));
        seriesRepository.create(series("angular", "angular", createArticle("a", PublicationStatus.DRAFT, null)));

        mockMvc.perform(get("/api/admin/series").contextPath("/api").with(user("admin")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].slug").value("angular"))
            .andExpect(jsonPath("$.content[0].chapterCount").value(1))
            .andExpect(jsonPath("$.content[1].slug").value("vue"));
        mockMvc.perform(get("/api/admin/series/999999").contextPath("/api").with(user("admin")))
            .andExpect(status().isNotFound());
        changeChapters(999_999L, List.of()).andExpect(status().isNotFound());
    }

    /**
     * Écriture concurrente : l'article rangé entre-temps dans une autre série est refusé par PostgreSQL, traduit.
     */
    @Test
    void translates_an_article_placed_meanwhile_in_another_series() {
        long taken = createArticle("deja-range", PublicationStatus.DRAFT, null);
        seriesRepository.create(series("autre", taken));
        Series series = seriesRepository.create(series("angular"));

        assertThatThrownBy(() -> seriesRepository.update(series.withChapters(List.of(taken))))
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.ARTICLE_ALREADY_IN_SERIES));
    }

    @Test
    void translates_a_slug_taken_meanwhile() {
        seriesRepository.create(series("angular"));

        assertThatThrownBy(() -> seriesRepository.create(series("angular")))
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.SLUG_ALREADY_USED));
    }

    @Test
    void translates_a_cover_deleted_meanwhile() {
        assertThatThrownBy(() -> seriesRepository.create(withCover(series("angular"), 999_999L)))
            .isInstanceOfSatisfying(InvalidInputException.class, exception ->
                assertThat(exception.field()).isEqualTo("coverMediaId"));
    }

    private long createArticle(String slug, PublicationStatus status, Instant publishedAt) {
        return publicationRepository.create(PublicationFixtures.article(slug, status, publishedAt)).id();
    }

    private ResultActions create(String json) throws Exception {
        return mockMvc.perform(post("/api/admin/series").contextPath("/api").with(user("admin")).with(xsrf())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions update(long id, String json) throws Exception {
        return mockMvc.perform(put("/api/admin/series/" + id).contextPath("/api").with(user("admin")).with(xsrf())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions changeChapters(long id, List<Long> publicationIds) throws Exception {
        return mockMvc.perform(put("/api/admin/series/" + id + "/chapters").contextPath("/api").with(user("admin"))
            .with(xsrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"publicationIds\":" + publicationIds + "}"));
    }

    private static String seriesJson(String title) {
        return "{\"title\":\"" + title + "\",\"descriptionMarkdown\":\"\"}";
    }

    private long idOf(String slug) {
        return seriesRepository.findBySlug(Slug.of(slug)).orElseThrow().id();
    }
}
