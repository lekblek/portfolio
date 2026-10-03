package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import com.scalke.portfolio.backend.publication.domain.model.PublicationAdminFilter;

/**
 * Toutes les publications, quel que soit leur statut, pour l'administration (D-CU) : les dernières modifiées d'abord.
 */
@Service
@RequiredArgsConstructor
public class ListPublicationsUseCase {

    private final PublicationRepository publicationRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResult<AdminPublication> execute(PublicationAdminFilter filter, PageQuery query) {
        Instant now = clock.instant();
        return publicationRepository.findPage(filter, now, query)
            .map(publication -> AdminPublication.at(publication, now));
    }
}
