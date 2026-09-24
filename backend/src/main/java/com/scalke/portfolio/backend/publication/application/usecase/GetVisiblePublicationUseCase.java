package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class GetVisiblePublicationUseCase {

    private final PublicationRepository publicationRepository;
    private final VisiblePublicationAssembler assembler;
    private final Clock clock;

    /**
     * Une publication inexistante, en brouillon, en relecture, archivée ou planifiée plus tard produit
     * la même erreur : l'API publique ne confirme pas l'existence d'un contenu invisible
     * ({@code docs/05-conventions-api.md} §9 et §29).
     */
    @Transactional(readOnly = true)
    public VisiblePublication execute(String slug) {
        return publicationRepository.findVisibleBySlug(slug, clock.instant())
            .map(assembler::assemble)
            .orElseThrow(() -> new ResourceNotFoundException(
                ErrorCode.RESOURCE_NOT_FOUND, "Publication introuvable."));
    }
}
