package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.model.SeriesContent;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crée une série sans chapitre (D-CV) : invisible tant qu'aucun de ses articles ne l'est (D-BG). Slug saisi ou généré
 * depuis le titre, puis premier libre (D-BD) ; couverture vérifiée.
 */
@Service
@RequiredArgsConstructor
public class CreateSeriesUseCase {

    private final SeriesRepository seriesRepository;
    private final SeriesAdministration administration;

    /**
     * @param slug slug saisi ; {@code null} : généré depuis le titre
     */
    @Transactional
    public AdminSeries execute(String slug, SeriesContent content) {
        administration.verifyCover(content.coverMediaId());
        Slug wanted = slug != null ? Slug.of(slug) : Slug.fromText(content.title());
        Slug free = wanted.firstAvailable(candidate -> seriesRepository.existsBySlug(candidate, null));
        return administration.view(seriesRepository.create(Series.newSeries(free, content)));
    }
}
