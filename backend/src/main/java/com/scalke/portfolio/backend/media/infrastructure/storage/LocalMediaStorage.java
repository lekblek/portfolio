package com.scalke.portfolio.backend.media.infrastructure.storage;

import com.scalke.portfolio.backend.media.domain.model.MediaContent;
import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import com.scalke.portfolio.backend.media.domain.port.MediaStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Stockage des médias sur le système de fichiers local (implémentation V1, D-BO) : un fichier par clé,
 * directement sous la racine configurée. Le répertoire doit être un volume persistant en production
 * (étape 52).
 */
@Component
@Slf4j
public class LocalMediaStorage implements MediaStorage {

    private final Path root;

    public LocalMediaStorage(MediaStorageProperties properties) {
        this.root = properties.storageRoot().toAbsolutePath().normalize();
        log.info("Stockage local des médias : {}", root);
    }

    @Override
    public Optional<MediaContent> open(StorageKey key) {
        Path file = resolve(key);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new MediaContent(Files.size(file), Files.newInputStream(file)));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read media " + key, e);
        }
    }

    /**
     * Le format de la clé exclut déjà toute sortie de la racine ; cette vérification est une seconde barrière.
     */
    private Path resolve(StorageKey key) {
        Path file = root.resolve(key.value()).normalize();
        if (!file.getParent().equals(root)) {
            throw new IllegalStateException("storage key " + key + " escapes the storage root");
        }
        return file;
    }
}
