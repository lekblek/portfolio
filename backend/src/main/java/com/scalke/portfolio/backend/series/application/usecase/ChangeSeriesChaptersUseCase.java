package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Remplace la liste ordonnée des chapitres d'une série (D-CV) : ajout, retrait et réordonnancement en une écriture,
 * positions renumérotées 1, 2, … Articles vérifiés par {@link SeriesAdministration#verifyChapters}.
 */
@Service
@RequiredArgsConstructor
public class ChangeSeriesChaptersUseCase {

    private final SeriesRepository seriesRepository;
    private final SeriesAdministration administration;

    @Transactional
    public AdminSeries execute(Long id, List<Long> publicationIds) {
        Series current = administration.find(id);
        administration.verifyChapters(id, publicationIds);
        return administration.view(seriesRepository.update(current.withChapters(publicationIds)));
    }
}
