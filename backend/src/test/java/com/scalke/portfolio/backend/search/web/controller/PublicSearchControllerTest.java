package com.scalke.portfolio.backend.search.web.controller;

import com.scalke.portfolio.backend.search.application.usecase.SearchPublicContentUseCase;
import com.scalke.portfolio.backend.search.application.usecase.SearchResult;
import com.scalke.portfolio.backend.shared.api.ApiPaging;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
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

@WebMvcTest(PublicSearchController.class)
class PublicSearchControllerTest {

    private static final SearchResult ARTICLE = new SearchResult(SearchResult.Type.ARTICLE, "Construire une API",
        Slug.of("construire-une-api"), "Résumé", Instant.parse("2026-06-01T09:00:00Z"));

    private static final SearchResult PROJECT = new SearchResult(SearchResult.Type.PROJECT, "Portfolio",
        Slug.of("portfolio"), "Description courte", null);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SearchPublicContentUseCase searchPublicContentUseCase;

    @Test
    void returns_a_page_of_results_with_their_type_and_an_explicit_null_date_for_a_project() throws Exception {
        given(searchPublicContentUseCase.execute(any(), any()))
            .willReturn(new PageResult<>(List.of(ARTICLE, PROJECT), 0, 10, 2));

        mockMvc.perform(get("/api/public/search").contextPath("/api").param("q", "api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].type").value("ARTICLE"))
            .andExpect(jsonPath("$.content[0].title").value("Construire une API"))
            .andExpect(jsonPath("$.content[0].slug").value("construire-une-api"))
            .andExpect(jsonPath("$.content[0].summary").value("Résumé"))
            .andExpect(jsonPath("$.content[0].publishedAt").value("2026-06-01T09:00:00Z"))
            .andExpect(jsonPath("$.content[1].type").value("PROJECT"))
            .andExpect(jsonPath("$.content[1].publishedAt").hasJsonPath())
            .andExpect(jsonPath("$.content[1].publishedAt").value(nullValue()))
            // ni identifiant ni pertinence (D-CE)
            .andExpect(jsonPath("$.content[0].id").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].rank").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.totalElements").value(2));

        then(searchPublicContentUseCase).should().execute("api", new PageQuery(0, ApiPaging.PUBLIC_PAGE_SIZE));
    }

    @Test
    void passes_the_requested_page_bounded_like_every_list() throws Exception {
        given(searchPublicContentUseCase.execute(any(), any())).willReturn(new PageResult<>(List.of(), 2, 100, 0));

        mockMvc.perform(get("/api/public/search").contextPath("/api")
                .param("q", "\"spring boot\" -java")
                .param("page", "2")
                .param("size", "500"))
            .andExpect(status().isOk());

        then(searchPublicContentUseCase).should()
            .execute("\"spring boot\" -java", new PageQuery(2, ApiPaging.MAX_PAGE_SIZE));
    }

    @Test
    void requires_a_search_text() throws Exception {
        mockMvc.perform(get("/api/public/search").contextPath("/api"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        then(searchPublicContentUseCase).should(never()).execute(any(), any());
    }

    /**
     * D-CF : 200 caractères au plus ; la borne est incluse.
     */
    @Test
    void rejects_a_search_text_longer_than_the_limit() throws Exception {
        given(searchPublicContentUseCase.execute(any(), any())).willReturn(new PageResult<>(List.of(), 0, 10, 0));
        String longest = "a".repeat(PublicSearchController.MAX_QUERY_LENGTH);

        mockMvc.perform(get("/api/public/search").contextPath("/api").param("q", longest))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/search").contextPath("/api").param("q", longest + "a"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("q"))
            .andExpect(jsonPath("$.errors[0].message").value("Le texte recherché ne doit pas dépasser 200 caractères."));

        then(searchPublicContentUseCase).should().execute(any(), any());
    }

    /**
     * Un texte vide n'est pas une erreur : la recherche ne trouve rien (D-CD).
     */
    @Test
    void accepts_an_empty_search_text() throws Exception {
        given(searchPublicContentUseCase.execute(any(), any())).willReturn(new PageResult<>(List.of(), 0, 10, 0));

        mockMvc.perform(get("/api/public/search").contextPath("/api").param("q", ""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());

        then(searchPublicContentUseCase).should().execute("", new PageQuery(0, ApiPaging.PUBLIC_PAGE_SIZE));
    }
}
