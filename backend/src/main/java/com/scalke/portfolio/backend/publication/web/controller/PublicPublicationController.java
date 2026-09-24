package com.scalke.portfolio.backend.publication.web.controller;

import com.scalke.portfolio.backend.publication.application.usecase.GetVisiblePublicationUseCase;
import com.scalke.portfolio.backend.publication.application.usecase.ListVisiblePublicationsUseCase;
import com.scalke.portfolio.backend.publication.domain.model.PublicationFilter;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.web.dto.PublicationResponse;
import com.scalke.portfolio.backend.publication.web.dto.PublicationSummaryResponse;
import com.scalke.portfolio.backend.shared.api.ApiPaging;
import com.scalke.portfolio.backend.shared.api.PageResponse;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/public/publications")
public class PublicPublicationController {

    private final ListVisiblePublicationsUseCase listVisiblePublicationsUseCase;
    private final GetVisiblePublicationUseCase getVisiblePublicationUseCase;

    /**
     * Pagination bornée comme pour les projets (D-V) ; ordre fixe, les plus récentes d'abord (D-AK).
     * <p>
     * {@code type} : {@code ARTICLE} ou {@code NEWS} ; absent, les deux. Une autre valeur est une
     * requête invalide (400 {@code MALFORMED_REQUEST}) : le type est un ensemble fermé du contrat (C09).
     */
    @GetMapping
    PageResponse<PublicationSummaryResponse> listPublications(
        @RequestParam(required = false) PublicationType type,
        @PageableDefault(size = ApiPaging.PUBLIC_PAGE_SIZE) Pageable pageable) {
        PageQuery query = new PageQuery(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.from(listVisiblePublicationsUseCase
            .execute(PublicationFilter.ofType(type), query)
            .map(PublicationSummaryResponse::from));
    }

    @GetMapping("/{slug}")
    PublicationResponse getPublication(@PathVariable String slug) {
        return PublicationResponse.from(getVisiblePublicationUseCase.execute(slug));
    }
}
