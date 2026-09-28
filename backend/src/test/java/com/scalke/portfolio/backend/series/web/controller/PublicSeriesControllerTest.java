package com.scalke.portfolio.backend.series.web.controller;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.series.application.usecase.GetVisibleSeriesUseCase;
import com.scalke.portfolio.backend.series.application.usecase.ListVisibleSeriesUseCase;
import com.scalke.portfolio.backend.series.application.usecase.SeriesChapter;
import com.scalke.portfolio.backend.series.application.usecase.SeriesSummary;
import com.scalke.portfolio.backend.series.application.usecase.VisibleSeries;
import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.model.SeriesItem;
import com.scalke.portfolio.backend.shared.api.ApiPaging;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicSeriesController.class)
class PublicSeriesControllerTest {

    private static final Series SPRING_BOOT = new Series(4L, "Spring Boot de zéro à la production",
        Slug.of("spring-boot-de-zero-a-la-production"), "## Une série",
        List.of(new SeriesItem(42L, 1), new SeriesItem(43L, 3)));

    private static final Publication CHAPTER = new Publication(
        43L, PublicationType.ARTICLE, "Sécuriser l'API", Slug.of("securiser-l-api"), "Résumé", "## Contenu",
        PublicationStatus.PUBLISHED, Instant.parse("2026-06-01T09:00:00Z"), Instant.parse("2026-06-01T09:00:00Z"),
        false, null, Set.of(), null, null, Instant.parse("2026-05-30T08:00:00Z"), Instant.parse("2026-06-01T09:00:00Z"));

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ListVisibleSeriesUseCase listVisibleSeriesUseCase;

    @MockitoBean
    GetVisibleSeriesUseCase getVisibleSeriesUseCase;

    @Test
    void lists_series_as_a_page_of_summaries() throws Exception {
        given(listVisibleSeriesUseCase.execute(any()))
            .willReturn(new PageResult<>(List.of(new SeriesSummary(SPRING_BOOT, 1)), 0, 10, 1));

        mockMvc.perform(get("/api/public/series").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].title").value("Spring Boot de zéro à la production"))
            .andExpect(jsonPath("$.content[0].slug").value("spring-boot-de-zero-a-la-production"))
            .andExpect(jsonPath("$.content[0].descriptionMarkdown").value("## Une série"))
            .andExpect(jsonPath("$.content[0].chapterCount").value(1))
            .andExpect(jsonPath("$.content[0].id").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].items").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.totalElements").value(1));

        then(listVisibleSeriesUseCase).should().execute(new PageQuery(0, ApiPaging.PUBLIC_PAGE_SIZE));
    }

    @Test
    void caps_the_page_size_at_the_api_maximum() throws Exception {
        given(listVisibleSeriesUseCase.execute(any())).willReturn(new PageResult<>(List.of(), 1, 100, 0));

        mockMvc.perform(get("/api/public/series").contextPath("/api").param("page", "1").param("size", "1000"))
            .andExpect(status().isOk());

        then(listVisibleSeriesUseCase).should().execute(new PageQuery(1, ApiPaging.MAX_PAGE_SIZE));
    }

    /**
     * D-BI : la position exposée est la position publique du chapitre, jamais la position stockée.
     */
    @Test
    void returns_the_series_with_its_table_of_contents() throws Exception {
        given(getVisibleSeriesUseCase.execute("spring-boot-de-zero-a-la-production"))
            .willReturn(new VisibleSeries(SPRING_BOOT, List.of(new SeriesChapter(1, CHAPTER))));

        mockMvc.perform(get("/api/public/series/spring-boot-de-zero-a-la-production").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Spring Boot de zéro à la production"))
            .andExpect(jsonPath("$.descriptionMarkdown").value("## Une série"))
            .andExpect(jsonPath("$.chapters.length()").value(1))
            .andExpect(jsonPath("$.chapters[0].position").value(1))
            .andExpect(jsonPath("$.chapters[0].title").value("Sécuriser l'API"))
            .andExpect(jsonPath("$.chapters[0].slug").value("securiser-l-api"))
            .andExpect(jsonPath("$.chapters[0].summary").value("Résumé"))
            .andExpect(jsonPath("$.chapters[0].publishedAt").value("2026-06-01T09:00:00Z"))
            .andExpect(jsonPath("$.chapters[0].readingTimeMinutes").value(1))
            .andExpect(jsonPath("$.chapters[0].contentMarkdown").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.chapters[0].id").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.id").doesNotHaveJsonPath());
    }

    @Test
    void returns_problem_details_when_the_series_is_not_visible() throws Exception {
        given(getVisibleSeriesUseCase.execute("angular"))
            .willThrow(new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Série introuvable."));

        mockMvc.perform(get("/api/public/series/angular").contextPath("/api"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.detail").value("Série introuvable."));
    }
}
