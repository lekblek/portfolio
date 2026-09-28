package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import com.scalke.portfolio.backend.media.domain.port.MediaStorage;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenMediaFileUseCase {

    private final MediaStorage mediaStorage;

    /**
     * Fichier public d'un média (D-BP). Une clé mal formée ou inconnue donne la même 404 ; la première sans
     * accéder au stockage.
     * <p>
     * Pas de {@code @Transactional} : aucune donnée n'est lue en base, une transaction ouvrirait une connexion
     * pour rien (exception assumée à ADR 0001, D-BQ).
     */
    public MediaFile execute(String storageKey) {
        StorageKey key = StorageKey.parse(storageKey).orElseThrow(OpenMediaFileUseCase::notFound);
        return mediaStorage.open(key)
            .map(content -> new MediaFile(key.format(), content))
            .orElseThrow(OpenMediaFileUseCase::notFound);
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Média introuvable.");
    }
}
