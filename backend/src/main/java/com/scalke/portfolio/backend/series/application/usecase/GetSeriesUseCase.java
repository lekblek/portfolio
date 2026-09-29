package com.scalke.portfolio.backend.series.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Une série et tous ses chapitres, quel que soit le statut de leurs articles, pour l'administration (D-CV).
 */
@Service
@RequiredArgsConstructor
public class GetSeriesUseCase {

    private final SeriesAdministration administration;

    @Transactional(readOnly = true)
    public AdminSeries execute(Long id) {
        return administration.view(administration.find(id));
    }
}
