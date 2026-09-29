package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Un média du catalogue, pour l'administration (D-CT).
 */
@Service
@RequiredArgsConstructor
public class GetMediaUseCase {

    private final MediaRepository mediaRepository;

    @Transactional(readOnly = true)
    public Media execute(Long id) {
        return mediaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Média introuvable."));
    }
}
