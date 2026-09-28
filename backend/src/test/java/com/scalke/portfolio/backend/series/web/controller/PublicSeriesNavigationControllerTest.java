package com.scalke.portfolio.backend.series.web.controller;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.series.application.usecase.GetSeriesNavigationUseCase;
import com.scalke.portfolio.backend.series.application.usecase.SeriesChapter;
import com.scalke.portfolio.backend.series.application.usecase.SeriesNavigation;
import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.model.SeriesItem;
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

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicSeriesNavigationController.class)
class PublicSeriesNavigationControllerTest {

    private static final Series SPRING_BOOT = new Series(4L, "Spring Boot de zéro à la production",
        Slug.of("spring-boot-de-zero-a-la-production"), "## Une série",
        List.of(new SeriesItem(41L, 1), new SeriesItem(42L, 2)));

    private static final Publication INTRODUCTION = new Publication(
        41L, PublicationType.ARTICLE, "Introduction", Slug.of("introduction"), "Résumé", "## Contenu",
        PublicationStatus.PUBLISHED, Instant.parse("2026-06-01T09:00:00Z"), Instant.parse("2026-06-01T09:00:00Z"),
        false, null, Set.of(), null, null, Instant.parse("2026-05-30T08:00:00Z"), Instant.parse("2026-06-01T09:00:00Z"));

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetSeriesNavigationUseCase getSeriesNavigationUseCase;

    /**
     * D-BL : position, nombre de chapitres, voisins ; {@code null} explicite pour le voisin absent (C10).
     */
    @Test
    void returns_the_series_navigation_of_a_publication() throws Exception {
        given(getSeriesNavigationUseCase.execute("configuration"))
            .willReturn(new SeriesNavigation(SPRING_BOOT, 2, 2, new SeriesChapter(1, INTRODUCTION), null));

        mockMvc.perform(get("/api/public/publications/configuration/series").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.series.title").value("Spring Boot de zéro à la production"))
            .andExpect(jsonPath("$.series.slug").value("spring-boot-de-zero-a-la-production"))
            .andExpect(jsonPath("$.series.descriptionMarkdown").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.position").value(2))
            .andExpect(jsonPath("$.chapterCount").value(2))
            .andExpect(jsonPath("$.previous.position").value(1))
            .andExpect(jsonPath("$.previous.title").value("Introduction"))
            .andExpect(jsonPath("$.previous.slug").value("introduction"))
            .andExpect(jsonPath("$.previous.summary").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.next").hasJsonPath())
            .andExpect(jsonPath("$.next").value(nullValue()));
    }

    @Test
    void returns_problem_details_when_the_publication_is_not_in_a_series() throws Exception {
        given(getSeriesNavigationUseCase.execute("annonce")).willThrow(new ResourceNotFoundException(
            ErrorCode.RESOURCE_NOT_FOUND, "Cette publication n'appartient à aucune série."));

        mockMvc.perform(get("/api/public/publications/annonce/series").contextPath("/api"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
