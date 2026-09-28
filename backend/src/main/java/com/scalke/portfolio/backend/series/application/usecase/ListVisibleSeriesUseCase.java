package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.media.application.query.MediaQueryService;
import com.scalke.portfolio.backend.media.application.query.PublicImage;
import com.scalke.portfolio.backend.publication.application.query.PublicationQueryService;
import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListVisibleSeriesUseCase {

    private final SeriesRepository seriesRepository;
    private final PublicationQueryService publications;
    private final MediaQueryService media;

    /**
     * Séries ayant au moins un article visible (D-BG), par titre. Le module {@code publication} dit quels
     * articles sont visibles ; la pagination et le comptage ne portent que sur les séries visibles. Sans
     * article visible, la page est vide sans chercher de série (D-BJ). Couvertures de toute la page en une requête,
     * aucune si la page n'en a pas (D-BY).
     */
    @Transactional(readOnly = true)
    public PageResult<SeriesSummary> execute(PageQuery query) {
        Set<Long> visible = publications.visibleIds(seriesRepository.findAllPublicationIds());
        if (visible.isEmpty()) {
            return PageResult.empty(query);
        }
        PageResult<Series> page = seriesRepository.findHavingAnyPublication(visible, query);
        Map<Long, PublicImage> covers = media.imagesById(page.content().stream()
            .map(Series::coverMediaId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()));
        return page.map(series -> new SeriesSummary(series, series.publicationIdsAmong(visible).size(),
            series.coverMediaId() == null ? null : covers.get(series.coverMediaId())));
    }
}
