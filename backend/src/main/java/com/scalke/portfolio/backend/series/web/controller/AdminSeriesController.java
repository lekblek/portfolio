package com.scalke.portfolio.backend.series.web.controller;

import com.scalke.portfolio.backend.series.application.usecase.AdminSeries;
import com.scalke.portfolio.backend.series.application.usecase.ChangeSeriesChaptersUseCase;
import com.scalke.portfolio.backend.series.application.usecase.CreateSeriesUseCase;
import com.scalke.portfolio.backend.series.application.usecase.GetSeriesUseCase;
import com.scalke.portfolio.backend.series.application.usecase.ListSeriesUseCase;
import com.scalke.portfolio.backend.series.application.usecase.UpdateSeriesUseCase;
import com.scalke.portfolio.backend.series.web.dto.AdminSeriesResponse;
import com.scalke.portfolio.backend.series.web.dto.AdminSeriesSummaryResponse;
import com.scalke.portfolio.backend.series.web.dto.ChangeSeriesChaptersRequest;
import com.scalke.portfolio.backend.series.web.dto.SaveSeriesRequest;
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
 * Administration des séries (D-CV), derrière la session de l'administrateur : saisie de la série, chapitres remplacés
 * d'un bloc par leur route propre. Pas de suppression : une série sans article visible n'apparaît pas sur le site.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/series")
public class AdminSeriesController {

    private final ListSeriesUseCase listSeriesUseCase;
    private final GetSeriesUseCase getSeriesUseCase;
    private final CreateSeriesUseCase createSeriesUseCase;
    private final UpdateSeriesUseCase updateSeriesUseCase;
    private final ChangeSeriesChaptersUseCase changeSeriesChaptersUseCase;

    @GetMapping
    PageResponse<AdminSeriesSummaryResponse> list(@PageableDefault(size = ApiPaging.ADMIN_PAGE_SIZE) Pageable pageable) {
        PageQuery query = new PageQuery(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.from(listSeriesUseCase.execute(query).map(AdminSeriesSummaryResponse::from));
    }

    @GetMapping("/{id}")
    AdminSeriesResponse get(@PathVariable Long id) {
        return AdminSeriesResponse.from(getSeriesUseCase.execute(id));
    }

    @PostMapping
    ResponseEntity<AdminSeriesResponse> create(@Valid @RequestBody SaveSeriesRequest body) {
        AdminSeries created = createSeriesUseCase.execute(body.slug(), body.toContent());
        return ResponseEntity
            .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.series().id()).toUri())
            .body(AdminSeriesResponse.from(created));
    }

    @PutMapping("/{id}")
    AdminSeriesResponse update(@PathVariable Long id, @Valid @RequestBody SaveSeriesRequest body) {
        return AdminSeriesResponse.from(updateSeriesUseCase.execute(id, body.slug(), body.toContent()));
    }

    /**
     * Ordre de lecture complet : 409 {@code NEWS_CANNOT_JOIN_SERIES} ou {@code ARTICLE_ALREADY_IN_SERIES}.
     */
    @PutMapping("/{id}/chapters")
    AdminSeriesResponse changeChapters(@PathVariable Long id, @Valid @RequestBody ChangeSeriesChaptersRequest body) {
        return AdminSeriesResponse.from(changeSeriesChaptersUseCase.execute(id, body.publicationIds()));
    }
}
