package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationContent;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Crée une publication en brouillon (D-CU). Slug saisi ou généré depuis le titre, puis premier libre (D-BD) ;
 * catégorie, tags et couverture vérifiés ({@link PublicationReferences}).
 */
@Service
@RequiredArgsConstructor
public class CreatePublicationUseCase {

    private final PublicationRepository publicationRepository;
    private final PublicationReferences publicationReferences;
    private final Clock clock;

    /**
     * @param slug slug saisi ; {@code null} : généré depuis le titre
     */
    @Transactional
    public AdminPublication execute(PublicationType type, String slug, PublicationContent content) {
        publicationReferences.verify(content);
        Slug wanted = slug != null ? Slug.of(slug) : Slug.fromText(content.title());
        Slug free = wanted.firstAvailable(candidate -> publicationRepository.existsBySlug(candidate, null));
        Instant now = clock.instant();
        return AdminPublication.at(publicationRepository.create(Publication.newDraft(type, free, content, now)), now);
    }
}
