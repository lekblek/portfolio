package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.publication.application.query.PublicationQueryService;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ListVisibleSeriesUseCase {

    private final SeriesRepository seriesRepository;
    private final PublicationQueryService publications;

    /**
     * Séries ayant au moins un article visible (D-BG), par titre. Le module {@code publication} dit quels
     * articles sont visibles ; la pagination et le comptage ne portent que sur les séries visibles. Sans
     * article visible, la page est vide sans chercher de série (D-BJ).
     */
    @Transactional(readOnly = true)
    public PageResult<SeriesSummary> execute(PageQuery query) {
        Set<Long> visible = publications.visibleIds(seriesRepository.findAllPublicationIds());
        if (visible.isEmpty()) {
            return PageResult.empty(query);
        }
        return seriesRepository.findHavingAnyPublication(visible, query)
            .map(series -> new SeriesSummary(series, series.publicationIdsAmong(visible).size()));
    }
}
