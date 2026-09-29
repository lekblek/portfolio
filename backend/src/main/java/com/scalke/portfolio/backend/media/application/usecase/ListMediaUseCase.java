package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalogue des médias pour l'administration, les plus récents d'abord (D-CT).
 */
@Service
@RequiredArgsConstructor
public class ListMediaUseCase {

    private final MediaRepository mediaRepository;

    @Transactional(readOnly = true)
    public PageResult<Media> execute(PageQuery query) {
        return mediaRepository.findPage(query);
    }
}
