package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.publication.application.query.PublicationQueryService;
import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class GetVisibleSeriesUseCase {

    private final SeriesRepository seriesRepository;
    private final PublicationQueryService publications;

    /**
     * Une série inexistante, au slug mal formé ou sans aucun article visible produit la même erreur :
     * l'API publique ne confirme pas l'existence d'une série invisible (D-BG, {@code 05} §9).
     */
    @Transactional(readOnly = true)
    public VisibleSeries execute(String slug) {
        Series series = Slug.parse(slug)
            .flatMap(seriesRepository::findBySlug)
            .orElseThrow(GetVisibleSeriesUseCase::notFound);
        Map<Long, Publication> visible = publications.visibleById(series.publicationIds());
        List<Long> chapters = series.publicationIdsAmong(visible.keySet());
        if (chapters.isEmpty()) {
            throw notFound();
        }
        return new VisibleSeries(series, IntStream.range(0, chapters.size())
            .mapToObj(index -> new SeriesChapter(index + 1, visible.get(chapters.get(index))))
            .toList());
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Série introuvable.");
    }
}
