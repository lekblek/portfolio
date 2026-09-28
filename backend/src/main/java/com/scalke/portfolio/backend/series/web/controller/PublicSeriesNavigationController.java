package com.scalke.portfolio.backend.series.web.controller;

import com.scalke.portfolio.backend.series.application.usecase.GetSeriesNavigationUseCase;
import com.scalke.portfolio.backend.series.web.dto.SeriesNavigationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sous-ressource d'une publication servie par le module {@code series} : le module {@code publication}
 * ignore les séries ({@code 04} §5), c'est le module propriétaire de la relation qui répond (D-BL).
 */
@RequiredArgsConstructor
@RestController
public class PublicSeriesNavigationController {

    private final GetSeriesNavigationUseCase getSeriesNavigationUseCase;

    @GetMapping("/public/publications/{slug}/series")
    SeriesNavigationResponse getSeriesNavigation(@PathVariable String slug) {
        return SeriesNavigationResponse.from(getSeriesNavigationUseCase.execute(slug));
    }
}
