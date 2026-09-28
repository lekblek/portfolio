package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import com.scalke.portfolio.backend.media.domain.port.MediaStorage;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteMediaUseCase {

    private final MediaRepository mediaRepository;
    private final MediaStorage mediaStorage;

    /**
     * Supprime un média qu'aucun contenu n'utilise (invariant 13, D-BV) : l'entrée du catalogue d'abord, que
     * PostgreSQL refuse si une référence existe ({@code MEDIA_STILL_REFERENCED}, 409), puis le fichier. Un
     * échec de suppression du fichier annule celle de l'entrée.
     * <p>
     * Pas de route HTTP avant l'administration protégée (étape 36, comme D-AU).
     */
    @Transactional
    public void execute(Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Média introuvable."));
        mediaRepository.delete(media);
        mediaStorage.delete(media.storageKey());
    }
}
