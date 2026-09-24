package com.scalke.portfolio.backend.publication.web.controller;

import com.scalke.portfolio.backend.publication.application.usecase.GetVisiblePublicationUseCase;
import com.scalke.portfolio.backend.publication.application.usecase.ListVisiblePublicationsUseCase;
import com.scalke.portfolio.backend.publication.application.usecase.PublicationCriteria;
import com.scalke.portfolio.backend.publication.application.usecase.VisiblePublication;
import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.shared.api.ApiPaging;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
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

    private static final VisiblePublication ARTICLE = new VisiblePublication(
        new Publication(
            42L, PublicationType.ARTICLE, "Construire une API", "construire-une-api", "Résumé", "## Contenu",
            PublicationStatus.PUBLISHED, Instant.parse("2026-06-01T09:00:00Z"), true, 3L, Set.of(5L, 6L),
            "Titre SEO", null, Instant.parse("2026-05-30T08:00:00Z"), Instant.parse("2026-06-01T09:00:00Z")),
        new Category(3L, "Backend", "backend", "Spring Boot, API, persistance."),
        List.of(new Tag(6L, "Java", "java"), new Tag(5L, "Spring Boot", "spring-boot")));

    private static final VisiblePublication UNCLASSIFIED_NEWS = new VisiblePublication(
        new Publication(
            43L, PublicationType.NEWS, "Lancement", "lancement", "Résumé", "Contenu",
            PublicationStatus.PUBLISHED, Instant.parse("2026-06-02T09:00:00Z"), false, null, Set.of(),
            null, null, Instant.parse("2026-06-02T09:00:00Z"), Instant.parse("2026-06-02T09:00:00Z")),
        null,
        List.of());

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ListVisiblePublicationsUseCase listVisiblePublicationsUseCase;

    @MockitoBean
    GetVisiblePublicationUseCase getVisiblePublicationUseCase;

    @Test
    void lists_publications_as_a_page_of_summaries() throws Exception {
        given(listVisiblePublicationsUseCase.execute(any(), any()))
            .willReturn(new PageResult<>(List.of(ARTICLE, UNCLASSIFIED_NEWS), 0, 10, 2));

        mockMvc.perform(get("/api/public/publications").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].type").value("ARTICLE"))
            .andExpect(jsonPath("$.content[0].slug").value("construire-une-api"))
            // instant ISO-8601 en UTC (05 §26)
            .andExpect(jsonPath("$.content[0].publishedAt").value("2026-06-01T09:00:00Z"))
            .andExpect(jsonPath("$.content[0].featured").value(true))
            .andExpect(jsonPath("$.content[0].readingTimeMinutes").value(1))
            // classement : nom et slug seulement (D-AP)
            .andExpect(jsonPath("$.content[0].category.name").value("Backend"))
            .andExpect(jsonPath("$.content[0].category.slug").value("backend"))
            .andExpect(jsonPath("$.content[0].category.id").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].category.description").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].tags[0].slug").value("java"))
            .andExpect(jsonPath("$.content[0].tags[1].name").value("Spring Boot"))
            // non classée : null et [] explicites (C10)
            .andExpect(jsonPath("$.content[1].category").hasJsonPath())
            .andExpect(jsonPath("$.content[1].category").value(nullValue()))
            .andExpect(jsonPath("$.content[1].tags").isEmpty())
            // pas de contenu dans la liste, aucun champ interne (D-AJ)
            .andExpect(jsonPath("$.content[0].contentMarkdown").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].id").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].status").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].categoryId").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].tagIds").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.content[0].createdAt").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.totalElements").value(2));

        then(listVisiblePublicationsUseCase).should()
            .execute(PublicationCriteria.none(), new PageQuery(0, ApiPaging.PUBLIC_PAGE_SIZE));
    }

    @Test
    void passes_every_filter_to_the_use_case() throws Exception {
        given(listVisiblePublicationsUseCase.execute(any(), any())).willReturn(new PageResult<>(List.of(), 0, 10, 0));

        mockMvc.perform(get("/api/public/publications").contextPath("/api")
                .param("type", "NEWS")
                .param("category", "backend")
                .param("tag", " spring-boot "))
            .andExpect(status().isOk());

        then(listVisiblePublicationsUseCase).should().execute(
            new PublicationCriteria(PublicationType.NEWS, "backend", "spring-boot"),
            new PageQuery(0, ApiPaging.PUBLIC_PAGE_SIZE));
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
            .andExpect(jsonPath("$.category.slug").value("backend"))
            .andExpect(jsonPath("$.tags.length()").value(2))
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
