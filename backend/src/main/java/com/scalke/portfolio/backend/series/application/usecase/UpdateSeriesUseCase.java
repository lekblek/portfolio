package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.model.SeriesContent;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remplace le titre, le slug, la description et la couverture d'une série (D-CV) ; ses chapitres ne changent pas.
 * Sans slug saisi, le slug est conservé ; un slug saisi et déjà pris est suffixé (D-BD), puis refusé si un article de
 * la série a déjà été public ({@code SLUG_LOCKED}, D-BK).
 */
@Service
@RequiredArgsConstructor
public class UpdateSeriesUseCase {

    private final SeriesRepository seriesRepository;
    private final SeriesAdministration administration;

    /**
     * @param slug slug saisi ; {@code null} : slug actuel conservé
     */
    @Transactional
    public AdminSeries execute(Long id, String slug, SeriesContent content) {
        Series current = administration.find(id);
        administration.verifyCover(content.coverMediaId());
        Slug wanted = slug != null ? Slug.of(slug) : current.slug();
        Slug free = wanted.firstAvailable(candidate -> seriesRepository.existsBySlug(candidate, id));
        boolean slugLocked = administration.view(current).slugLocked();
        return administration.view(seriesRepository.update(current.edit(free, content, slugLocked)));
    }
}
