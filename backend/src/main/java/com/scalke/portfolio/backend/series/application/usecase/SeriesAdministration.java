package com.scalke.portfolio.backend.series.application.usecase;

import com.scalke.portfolio.backend.media.application.query.MediaQueryService;
import com.scalke.portfolio.backend.publication.application.query.PublicationQueryService;
import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.model.SeriesItem;
import com.scalke.portfolio.backend.series.domain.port.SeriesRepository;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.InvalidInputException;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Règles communes aux cas d'usage d'administration des séries (D-CV) : lecture complète, vérification des
 * références par les façades des modules propriétaires (ADR 0002).
 */
@Component
@RequiredArgsConstructor
class SeriesAdministration {

    private final SeriesRepository seriesRepository;
    private final PublicationQueryService publications;
    private final MediaQueryService media;
    private final Clock clock;

    Series find(Long id) {
        return seriesRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Série introuvable."));
    }

    /**
     * La série avec tous ses chapitres ; slug verrouillé si l'un de ses articles a déjà été public (D-BK). Une requête
     * pour les articles, quel que soit leur nombre.
     */
    AdminSeries view(Series series) {
        Instant now = clock.instant();
        Map<Long, Publication> articles = publications.byId(series.publicationIds());
        boolean slugLocked = articles.values().stream().anyMatch(article -> article.hasBeenPublic(now));
        List<AdminSeries.Chapter> chapters = series.items().stream()
            .map(item -> chapter(item, articles.get(item.publicationId()), now))
            .toList();
        return new AdminSeries(series, slugLocked, chapters);
    }

    private static AdminSeries.Chapter chapter(SeriesItem item, Publication article, Instant now) {
        return new AdminSeries.Chapter(item.position(), article, article.effectiveStatus(now));
    }

    /**
     * Couverture facultative ; sinon une image du catalogue : un PDF est refusé comme un identifiant inconnu (D-BV).
     */
    void verifyCover(Long coverMediaId) {
        if (coverMediaId != null && !media.imagesById(Set.of(coverMediaId)).containsKey(coverMediaId)) {
            throw new InvalidInputException("coverMediaId", "Image de couverture inconnue.");
        }
    }

    /**
     * Chapitres proposés pour la série {@code seriesId} : chaque article au plus une fois, existant (400 sur le champ),
     * de type {@code ARTICLE} ({@code NEWS_CANNOT_JOIN_SERIES}, invariant 1), rangé dans aucune autre série
     * ({@code ARTICLE_ALREADY_IN_SERIES}, invariant 2).
     */
    void verifyChapters(Long seriesId, List<Long> publicationIds) {
        if (new HashSet<>(publicationIds).size() != publicationIds.size()) {
            throw new InvalidInputException("publicationIds", "Un article figure deux fois.");
        }
        Map<Long, Publication> articles = publications.byId(publicationIds);
        if (!articles.keySet().containsAll(publicationIds)) {
            throw new InvalidInputException("publicationIds", "Publication inconnue.");
        }
        if (articles.values().stream().anyMatch(publication -> publication.type() != PublicationType.ARTICLE)) {
            throw new BusinessRuleViolationException(ErrorCode.NEWS_CANNOT_JOIN_SERIES,
                "Une actualité ne peut pas appartenir à une série.");
        }
        if (!seriesRepository.findPublicationIdsInOtherSeries(publicationIds, seriesId).isEmpty()) {
            throw new BusinessRuleViolationException(ErrorCode.ARTICLE_ALREADY_IN_SERIES,
                "Un article appartient déjà à une autre série.");
        }
    }
}
