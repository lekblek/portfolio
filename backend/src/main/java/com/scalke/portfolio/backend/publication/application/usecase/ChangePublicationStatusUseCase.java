package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Change le statut éditorial d'une publication ({@code docs/05-conventions-api.md} §17).
 * <p>
 * Opération d'administration : elle sera exposée par {@code POST /api/admin/publications/{id}/status}
 * à l'étape 36, derrière l'authentification (étapes 32 à 35), jamais avant (D-AU). Les règles de
 * transition vivent dans le domaine ({@link Publication#transitionTo}) ; une transition refusée donne
 * 409 {@code INVALID_PUBLICATION_TRANSITION}.
 */
@Service
@RequiredArgsConstructor
public class ChangePublicationStatusUseCase {

    private final PublicationRepository publicationRepository;
    private final Clock clock;

    /**
     * @param scheduledAt date de publication pour une planification ({@code SCHEDULED}) ; {@code null} sinon
     */
    @Transactional
    public Publication execute(Long publicationId, PublicationStatus target, Instant scheduledAt) {
        Publication publication = publicationRepository.findById(publicationId)
            .orElseThrow(() -> new ResourceNotFoundException(
                ErrorCode.RESOURCE_NOT_FOUND, "Publication introuvable."));
        return publicationRepository.updateStatus(publication.transitionTo(target, scheduledAt, clock.instant()));
    }
}
