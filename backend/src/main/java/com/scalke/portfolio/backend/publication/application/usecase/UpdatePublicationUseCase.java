package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationContent;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Remplace la saisie d'une publication (D-CU) ; son type, son statut et ses dates ne changent pas. Sans slug saisi,
 * le slug est conservé ; un slug saisi et déjà pris est suffixé (D-BD), puis refusé si la publication a déjà été
 * publique ({@code SLUG_LOCKED}, D-BC).
 */
@Service
@RequiredArgsConstructor
public class UpdatePublicationUseCase {

    private final PublicationRepository publicationRepository;
    private final PublicationReferences publicationReferences;
    private final Clock clock;

    /**
     * @param slug slug saisi ; {@code null} : slug actuel conservé
     */
    @Transactional
    public AdminPublication execute(Long id, String slug, PublicationContent content) {
        Publication current = publicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Publication introuvable."));
        publicationReferences.verify(content);
        Slug wanted = slug != null ? Slug.of(slug) : current.slug();
        Slug free = wanted.firstAvailable(candidate -> publicationRepository.existsBySlug(candidate, id));
        Instant now = clock.instant();
        return AdminPublication.at(publicationRepository.update(current.edit(free, content, now)), now);
    }
}
