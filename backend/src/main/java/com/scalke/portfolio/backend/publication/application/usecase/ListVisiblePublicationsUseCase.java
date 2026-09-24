package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationFilter;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class ListVisiblePublicationsUseCase {

    private final PublicationRepository publicationRepository;
    private final Clock clock;

    /**
     * La visibilité est calculée à la lecture (D03) avec l'horloge applicative.
     */
    @Transactional(readOnly = true)
    public PageResult<Publication> execute(PublicationFilter filter, PageQuery query) {
        return publicationRepository.findVisible(filter, clock.instant(), query);
    }
}
