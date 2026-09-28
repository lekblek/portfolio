package com.scalke.portfolio.backend.media;

import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Catalogue en mémoire pour les tests unitaires des cas d'usage : attribue des identifiants croissants.
 */
public class InMemoryMediaRepository implements MediaRepository {

    private final List<Media> media = new ArrayList<>();

    @Override
    public Media create(Media created) {
        Media stored = new Media((long) media.size() + 1, created.storageKey(), created.originalName(), created.size(),
            created.dimensions(), created.altText(), created.createdAt());
        media.add(stored);
        return stored;
    }

    public List<Media> media() {
        return media;
    }
}
