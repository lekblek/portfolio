package com.scalke.portfolio.backend.media.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Configuration du stockage local ({@code portfolio.media.*}). {@code storageRoot} : répertoire racine des
 * fichiers, relatif au répertoire de travail s'il n'est pas absolu (D-BO). Une valeur vide est refusée : elle
 * désignerait le répertoire de travail lui-même (variable {@code MEDIA_STORAGE_ROOT} définie sans valeur).
 */
@ConfigurationProperties("portfolio.media")
public record MediaStorageProperties(Path storageRoot) {

    public MediaStorageProperties {
        Objects.requireNonNull(storageRoot, "portfolio.media.storage-root");
        if (storageRoot.toString().isBlank()) {
            throw new IllegalArgumentException("portfolio.media.storage-root must not be empty");
        }
    }
}
