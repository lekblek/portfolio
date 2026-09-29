package com.scalke.portfolio.backend.search.web.controller;

import com.scalke.portfolio.backend.search.application.usecase.SearchPublicContentUseCase;
import com.scalke.portfolio.backend.search.web.dto.SearchResultResponse;
import com.scalke.portfolio.backend.shared.api.ApiPaging;
import com.scalke.portfolio.backend.shared.api.PageResponse;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/public/search")
public class PublicSearchController {

    /**
     * Longueur maximale du texte recherché (D-CF) : largement assez pour une saisie de moteur de recherche,
     * et borne le travail de {@code websearch_to_tsquery}.
     */
    static final int MAX_QUERY_LENGTH = 200;

    private final SearchPublicContentUseCase searchPublicContentUseCase;

    /**
     * Articles, news et projets publics classés ensemble par pertinence (D-CD) ; pagination bornée comme pour les
     * listes (D-V), {@code sort} ignoré. {@code q} obligatoire (absent → 400 {@code MALFORMED_REQUEST}), lu comme une
     * recherche web, 200 caractères au plus (au-delà → 400 {@code VALIDATION_FAILED}) ; vide ou blanc, il ne trouve
     * rien (D-CF).
     */
    @GetMapping
    PageResponse<SearchResultResponse> search(
        @RequestParam @Size(max = MAX_QUERY_LENGTH, message = "Le texte recherché ne doit pas dépasser {max} caractères.")
        String q,
        @PageableDefault(size = ApiPaging.PUBLIC_PAGE_SIZE) Pageable pageable) {
        PageQuery query = new PageQuery(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.from(searchPublicContentUseCase.execute(q, query).map(SearchResultResponse::from));
    }
}
