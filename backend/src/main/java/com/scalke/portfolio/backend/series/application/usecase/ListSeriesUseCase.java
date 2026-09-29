package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Toutes les séries, visibles ou non, pour l'administration (D-CV), par titre.
 */
@Service
@RequiredArgsConstructor
public class ListSeriesUseCase {

    private final SeriesRepository seriesRepository;

    @Transactional(readOnly = true)
    public PageResult<Series> execute(PageQuery query) {
        return seriesRepository.findPage(query);
    }
}
