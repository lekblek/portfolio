package com.scalke.portfolio.backend.media;

import com.scalke.portfolio.backend.media.domain.model.MediaContent;
import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import com.scalke.portfolio.backend.media.domain.port.MediaStorage;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stockage en mémoire pour les tests unitaires des cas d'usage (D-BQ) : même contrat que
 * {@code LocalMediaStorage}, et trace des clés lues.
 */
public class InMemoryMediaStorage implements MediaStorage {

    private final Map<StorageKey, byte[]> files = new HashMap<>();
    private final List<StorageKey> opened = new ArrayList<>();

    @Override
    public Optional<MediaContent> open(StorageKey key) {
        opened.add(key);
        return Optional.ofNullable(files.get(key))
            .map(content -> new MediaContent(content.length, new ByteArrayInputStream(content)));
    }

    @Override
    public void store(StorageKey key, byte[] content) {
        if (files.putIfAbsent(key, content.clone()) != null) {
            throw new IllegalStateException("storage key " + key + " is already used");
        }
    }

    public Map<StorageKey, byte[]> files() {
        return files;
    }

    public List<StorageKey> opened() {
        return opened;
    }
}
