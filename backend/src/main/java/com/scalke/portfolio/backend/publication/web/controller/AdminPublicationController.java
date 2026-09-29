package com.scalke.portfolio.backend.publication.web.controller;

import com.scalke.portfolio.backend.publication.application.usecase.AdminPublication;
import com.scalke.portfolio.backend.publication.application.usecase.ChangePublicationStatusUseCase;
import com.scalke.portfolio.backend.publication.application.usecase.CreatePublicationUseCase;
import com.scalke.portfolio.backend.publication.application.usecase.GetPublicationUseCase;
import com.scalke.portfolio.backend.publication.application.usecase.ListPublicationsUseCase;
import com.scalke.portfolio.backend.publication.application.usecase.UpdatePublicationUseCase;
import com.scalke.portfolio.backend.publication.web.dto.AdminPublicationResponse;
import com.scalke.portfolio.backend.publication.web.dto.AdminPublicationSummaryResponse;
import com.scalke.portfolio.backend.publication.web.dto.ChangePublicationStatusRequest;
import com.scalke.portfolio.backend.publication.web.dto.CreatePublicationRequest;
import com.scalke.portfolio.backend.publication.web.dto.UpdatePublicationRequest;
import com.scalke.portfolio.backend.shared.api.ApiPaging;
import com.scalke.portfolio.backend.shared.api.PageResponse;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Administration des publications (D-CU), derrière la session de l'administrateur : tous les statuts, création en
 * brouillon, modification de la saisie, changement de statut par sa route propre ({@code 05} §17). Pas de
 * suppression : l'archivage retire une publication du site.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/publications")
public class AdminPublicationController {

    private final ListPublicationsUseCase listPublicationsUseCase;
    private final GetPublicationUseCase getPublicationUseCase;
    private final CreatePublicationUseCase createPublicationUseCase;
    private final UpdatePublicationUseCase updatePublicationUseCase;
    private final ChangePublicationStatusUseCase changePublicationStatusUseCase;

    @GetMapping
    PageResponse<AdminPublicationSummaryResponse> list(
        @PageableDefault(size = ApiPaging.ADMIN_PAGE_SIZE) Pageable pageable) {
        PageQuery query = new PageQuery(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.from(listPublicationsUseCase.execute(query).map(AdminPublicationSummaryResponse::from));
    }

    @GetMapping("/{id}")
    AdminPublicationResponse get(@PathVariable Long id) {
        return AdminPublicationResponse.from(getPublicationUseCase.execute(id));
    }

    @PostMapping
    ResponseEntity<AdminPublicationResponse> create(@Valid @RequestBody CreatePublicationRequest body) {
        AdminPublication created = createPublicationUseCase.execute(body.type(), body.slug(), body.toContent());
        return ResponseEntity
            .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.publication().id()).toUri())
            .body(AdminPublicationResponse.from(created));
    }

    @PutMapping("/{id}")
    AdminPublicationResponse update(@PathVariable Long id, @Valid @RequestBody UpdatePublicationRequest body) {
        return AdminPublicationResponse.from(updatePublicationUseCase.execute(id, body.slug(), body.toContent()));
    }

    /**
     * Transition du cycle éditorial (D-AU, D-AV) : 409 {@code INVALID_PUBLICATION_TRANSITION} si elle est interdite.
     */
    @PostMapping("/{id}/status")
    AdminPublicationResponse changeStatus(@PathVariable Long id,
                                          @Valid @RequestBody ChangePublicationStatusRequest body) {
        return AdminPublicationResponse.from(
            changePublicationStatusUseCase.execute(id, body.status(), body.publishedAt()));
    }
}
