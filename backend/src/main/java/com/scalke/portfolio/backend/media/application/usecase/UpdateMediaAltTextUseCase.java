package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remplace le texte alternatif d'un média (D-CT). Le fichier et ses métadonnées techniques ne changent jamais :
 * un autre fichier est un autre média.
 */
@Service
@RequiredArgsConstructor
public class UpdateMediaAltTextUseCase {

    private final MediaRepository mediaRepository;

    @Transactional
    public Media execute(Long id, String altText) {
        Media media = mediaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Média introuvable."));
        return mediaRepository.updateAltText(media.withAltText(Media.normalizeAltText(altText)));
    }
}
