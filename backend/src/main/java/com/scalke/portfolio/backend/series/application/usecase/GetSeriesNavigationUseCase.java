package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.publication.application.query.PublicationQueryService;
import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.series.domain.model.ChapterNavigation;
import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GetSeriesNavigationUseCase {

    private final SeriesRepository seriesRepository;
    private final PublicationQueryService publications;

    /**
     * Navigation depuis un article (D-BL) : précédent, suivant et progression parmi les seuls articles
     * visibles de sa série (D-BM). Une publication inconnue, invisible ou au slug mal formé donne la même
     * erreur que la lecture publique ; une publication visible hors de toute série donne une autre 404, qui
     * ne révèle rien de caché.
     */
    @Transactional(readOnly = true)
    public SeriesNavigation execute(String publicationSlug) {
        Publication article = Slug.parse(publicationSlug)
            .flatMap(publications::findVisibleBySlug)
            .orElseThrow(GetSeriesNavigationUseCase::publicationNotFound);
        Series series = seriesRepository.findByPublicationId(article.id())
            .orElseThrow(() -> new ResourceNotFoundException(
                ErrorCode.RESOURCE_NOT_FOUND, "Cette publication n'appartient à aucune série."));
        Map<Long, Publication> visible = publications.visibleById(series.publicationIds());
        ChapterNavigation navigation = series.navigationAround(article.id(), visible.keySet())
            .orElseThrow(GetSeriesNavigationUseCase::publicationNotFound);
        return new SeriesNavigation(
            series,
            navigation.position(),
            navigation.chapterCount(),
            chapter(navigation.position() - 1, navigation.previousPublicationId(), visible),
            chapter(navigation.position() + 1, navigation.nextPublicationId(), visible));
    }

    private static SeriesChapter chapter(int position, Long publicationId, Map<Long, Publication> visible) {
        return publicationId == null ? null : new SeriesChapter(position, visible.get(publicationId));
    }

    private static ResourceNotFoundException publicationNotFound() {
        return new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Publication introuvable.");
    }
}
