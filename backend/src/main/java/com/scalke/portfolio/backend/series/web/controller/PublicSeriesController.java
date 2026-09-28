package com.scalke.portfolio.backend.series.web.controller;

import com.scalke.portfolio.backend.series.application.usecase.GetVisibleSeriesUseCase;
import com.scalke.portfolio.backend.series.application.usecase.ListVisibleSeriesUseCase;
import com.scalke.portfolio.backend.series.web.dto.SeriesResponse;
import com.scalke.portfolio.backend.series.web.dto.SeriesSummaryResponse;
import com.scalke.portfolio.backend.shared.api.ApiPaging;
import com.scalke.portfolio.backend.shared.api.PageResponse;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/public/series")
public class PublicSeriesController {

    private final ListVisibleSeriesUseCase listVisibleSeriesUseCase;
    private final GetVisibleSeriesUseCase getVisibleSeriesUseCase;

    /**
     * Pagination bornée comme pour les autres listes (D-V) ; ordre fixe par titre (D-BI).
     */
    @GetMapping
    PageResponse<SeriesSummaryResponse> listSeries(@PageableDefault(size = ApiPaging.PUBLIC_PAGE_SIZE) Pageable pageable) {
        PageQuery query = new PageQuery(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.from(listVisibleSeriesUseCase.execute(query).map(SeriesSummaryResponse::from));
    }

    @GetMapping("/{slug}")
    SeriesResponse getSeries(@PathVariable String slug) {
        return SeriesResponse.from(getVisibleSeriesUseCase.execute(slug));
    }
}
