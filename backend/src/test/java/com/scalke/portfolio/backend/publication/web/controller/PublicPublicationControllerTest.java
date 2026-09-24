package com.scalke.portfolio.backend.publication.web.controller;

import com.scalke.portfolio.backend.publication.application.usecase.GetVisiblePublicationUseCase;
import com.scalke.portfolio.backend.publication.application.usecase.ListVisiblePublicationsUseCase;
import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationFilter;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.shared.api.ApiPaging;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
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

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicPublicationController.class)
class PublicPublicationControllerTest {

    private static final Publication ARTICLE = new Publication(
        42L, PublicationType.ARTICLE, "Construire une API", "construire-une-api", "Résumé", "## Contenu",
        PublicationStatus.PUBLISHED, Instant.parse("2026-06-01T09:00:00Z"), true, "Titre SEO", null,
        Instant.parse("2026-05-30T08:00:00Z"), Instant.parse("2026-06-01T09:00:00Z"));

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ListVisiblePublicationsUseCase listVisiblePublicationsUseCase;

    @MockitoBean
    GetVisiblePublicationUseCase getVisiblePublicationUseCase;

    @Test
    void lists_publications_as_a_page_of_summaries() throws Exception {
        given(listVisiblePublicationsUseCase.execute(any(), any()))
            .willReturn(new PageResult<>(List.of(ARTICLE), 0, 10, 1));

        mockMvc.perform(get("/api/public/publications").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].type").value("ARTICLE"))
            .andExpect(jsonPath("$.content[0].slug").value("construire-une-api"))
            // instant ISO-8601 en UTC (05 §26)
            .andExpect(jsonPath("$.content[0].publishedAt").value("2026-06-01T09:00:00Z"))
            .andExpect(jsonPath("$.content[0].featured").value(true))
            .andExpect(jsonPath("$.content[0].readingTimeMinutes").value(1))
            // pas de contenu dans la liste, aucun champ interne (D-AJ)
            .andExpect(jsonPath("$.content[0].contentMarkdown").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].id").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].status").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].createdAt").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].updatedAt").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.totalElements").value(1));

        then(listVisiblePublicationsUseCase).should()
            .execute(PublicationFilter.none(), new PageQuery(0, ApiPaging.PUBLIC_PAGE_SIZE));
    }

    @Test
    void passes_the_type_filter_to_the_use_case() throws Exception {
        given(listVisiblePublicationsUseCase.execute(any(), any())).willReturn(new PageResult<>(List.of(), 0, 10, 0));

        mockMvc.perform(get("/api/public/publications").contextPath("/api").param("type", "NEWS"))
            .andExpect(status().isOk());

        then(listVisiblePublicationsUseCase).should()
            .execute(PublicationFilter.ofType(PublicationType.NEWS), new PageQuery(0, ApiPaging.PUBLIC_PAGE_SIZE));
    }

    /**
     * D-AK : le type est un ensemble fermé ; une valeur inconnue est une requête invalide, pas une page vide.
     */
    @Test
    void rejects_an_unknown_type_as_a_malformed_request() throws Exception {
        mockMvc.perform(get("/api/public/publications").contextPath("/api").param("type", "TUTORIAL"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        then(listVisiblePublicationsUseCase).should(never()).execute(any(), any());
    }

    @Test
    void returns_the_publication_detail_with_explicit_nulls() throws Exception {
        given(getVisiblePublicationUseCase.execute("construire-une-api")).willReturn(ARTICLE);

        mockMvc.perform(get("/api/public/publications/construire-une-api").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contentMarkdown").value("## Contenu"))
            .andExpect(jsonPath("$.seoTitle").value("Titre SEO"))
            .andExpect(jsonPath("$.seoDescription").hasJsonPath())
            .andExpect(jsonPath("$.seoDescription").value(nullValue()))
            .andExpect(jsonPath("$.status").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.id").doesNotHaveJsonPath());
    }

    @Test
    void returns_problem_details_when_the_publication_is_not_visible() throws Exception {
        given(getVisiblePublicationUseCase.execute("brouillon"))
            .willThrow(new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Publication introuvable."));

        mockMvc.perform(get("/api/public/publications/brouillon").contextPath("/api"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
