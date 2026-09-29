package com.scalke.portfolio.backend.media.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Configuration du stockage local ({@code portfolio.media.*}). {@code storageRoot} : répertoire racine des
 * fichiers, relatif au répertoire de travail s'il n'est pas absolu (D-BO). Absente ou vide, elle est refusée au
 * démarrage : elle désignerait le répertoire de travail lui-même, perdu au redéploiement d'un conteneur (D-DD).
 */
@ConfigurationProperties("portfolio.media")
public record MediaStorageProperties(Path storageRoot) {

    public MediaStorageProperties {
        // Obligatoire hors dev (D-DD) : sans défaut, une variable absente arrive vide, donc nulle.
        if (storageRoot == null || storageRoot.toString().isBlank()) {
            throw new IllegalArgumentException(
                "portfolio.media.storage-root (MEDIA_STORAGE_ROOT) must be set to a persistent directory");
        }
    }
}
