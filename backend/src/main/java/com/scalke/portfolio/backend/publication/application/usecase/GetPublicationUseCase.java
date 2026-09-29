package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Une publication, quel que soit son statut, pour l'administration (D-CU).
 */
@Service
@RequiredArgsConstructor
public class GetPublicationUseCase {

    private final PublicationRepository publicationRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AdminPublication execute(Long id) {
        return publicationRepository.findById(id)
            .map(publication -> AdminPublication.at(publication, clock.instant()))
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Publication introuvable."));
    }
}
