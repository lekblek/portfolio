package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Envoi d'un média de démonstration du profil {@code dev} (D-EU) : le média qui porte déjà ce nom d'origine s'il
 * existe, sinon l'envoi complet ({@link UploadMediaUseCase}, mêmes vérifications). Un redémarrage ne duplique donc
 * aucun fichier, et plusieurs contenus partagent la même image. Le flux n'est lu que si l'envoi a lieu.
 */
@Service
@RequiredArgsConstructor
public class UploadMediaOnceUseCase {

    private final MediaRepository mediaRepository;
    private final UploadMediaUseCase uploadMediaUseCase;

    @Transactional
    public Media execute(MediaUpload upload) {
        return mediaRepository.findFirstByOriginalName(upload.originalName())
            .orElseGet(() -> uploadMediaUseCase.execute(upload));
    }
}
